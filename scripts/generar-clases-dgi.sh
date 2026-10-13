#!/bin/bash
#
# Genera las clases JAXB de DGI (dgi.classes.*) desde los XSD de una version, reproduciendo como se generaron en 2016
# (xjc a mano + cambios a mano, que aca son parches documentados).
#
# Uso: scripts/generar-clases-dgi.sh [carpeta de los XSD] [carpeta de salida]
#   por defecto resources/xsd/XSDs_FE_1_44_2 y app (escribe app/dgi/classes/...)
#
# Para revisar antes de pisar: generar en otra carpeta y comparar con app/dgi/classes.
#
set -euo pipefail

BASE=$(cd "$(dirname "$0")/.." && pwd)
XSDS=${1:-$BASE/resources/xsd/XSDs_FE_1_44_2}
SALIDA=${2:-$BASE/app}
# xjc del JDK 8 (el de la app); en Java 11+ no viene
export JAVA_HOME=${JAVA_HOME_8:-/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home}
XJC="$JAVA_HOME/bin/xjc"

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

# Copia de los XSD sin el DOCTYPE de xmldsig y xenc: apunta a un DTD por http que xjc no baja
cp "$XSDS"/original/*.xsd "$TMP"/
cp "$XSDS"/bindings-recepcion.xjb "$TMP"/
python3 - "$TMP" <<'EOF'
import re, sys, pathlib
for nombre in ("xmldsig-core-schema.xsd", "xenc-schema.xsd"):
    p = pathlib.Path(sys.argv[1]) / nombre
    s = p.read_text(encoding="utf-8")
    p.write_text(re.sub(r"<!DOCTYPE[^\[>]*(\[.*?\])?\s*>", "", s, count=1, flags=re.S), encoding="utf-8")
EOF

GEN="$TMP/gen"
mkdir -p "$GEN"
xjc() { "$XJC" -encoding UTF-8 -quiet -d "$GEN" "$@"; }

cd "$TMP"
# CFE y sobre a DGI; CFE entre empresas: modo simple y renombres (ver bindings-recepcion.xjb)
xjc -extension -b bindings-recepcion.xjb -p dgi.classes.recepcion EnvioCFE.xsd
xjc -extension -b bindings-recepcion.xjb -p dgi.classes.entreEmpresas EnvioCFE_entreEmpresas.xsd
# Reporte diario y respuestas: modo normal (clases con sufijo Type)
xjc -p dgi.classes.reporte ReporteDiarioCFE.xsd
# ACKCFE_DGI y no ACKCFE_Partes: es el mismo tipo con ObsCFC e InfoRespOri, asi tambien lee las respuestas de empresas
xjc -p dgi.classes.respuestas.cfe ACKCFE_DGI.xsd
xjc -p dgi.classes.respuestas.sobre ACKSobre.xsd
xjc -p dgi.classes.respuestas.reporte ACKRepDiario.xsd

# Parches (los cambios que se le hicieron a mano a las clases generadas en 2016)
python3 - "$GEN/dgi/classes" "$BASE/app/dgi/classes" <<'EOF'
import re, sys, pathlib
gen, actual = pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2])

def editar(rel, *cambios):
    p = gen / rel
    s = p.read_text(encoding="utf-8")
    for viejo, nuevo in cambios:
        if not re.search(viejo, s):
            sys.exit("parche: no se encontro %r en %s" % (viejo, rel))
        s = re.sub(viejo, nuevo, s)
    p.write_text(s, encoding="utf-8")

# 1. entreEmpresas usa el CFEDefType de recepcion (el CFE es el mismo; asi el codigo maneja una sola clase)
(gen / "entreEmpresas/CFEDefType.java").unlink()
for p in (gen / "entreEmpresas").glob("*.java"):
    s = p.read_text(encoding="utf-8")
    if re.search(r"\bCFEDefType\b", s):
        p.write_text(re.sub(r"(package dgi\.classes\.entreEmpresas;\n)", r"\1\nimport dgi.classes.recepcion.CFEDefType;\n", s, count=1), encoding="utf-8")

# 2. Valores base64 como String (el texto tal cual viene en el XML): el hash del CFE sale del DigestValue de la firma,
#    y el token de la consulta de un sobre se guarda y se reenvia como texto
def a_string(rel, campo):
    nombre = campo[0].upper() + campo[1:]
    editar(rel, (r"protected byte\[\] %s;" % campo, "protected String %s;" % campo),
           (r"public byte\[\] get%s\(\)" % nombre, "public String get%s()" % nombre),
           (r"public void set%s\(byte\[\] value\)" % nombre, "public void set%s(String value)" % nombre))
a_string("recepcion/DsigReferenceType.java", "digestValue")
a_string("respuestas/sobre/ParamConsultaType.java", "token")

# 3. @XmlRootElement en los tipos que la app lee o escribe como documento (xjc no lo pone: el elemento usa un tipo con nombre)
def raiz(rel, clase, elemento):
    editar(rel, (r"\npublic class %s\b" % clase, '\n@XmlRootElement(name = "%s")\npublic class %s' % (elemento, clase)),
           (r"(import javax\.xml\.bind\.annotation\.XmlType;)", r"import javax.xml.bind.annotation.XmlRootElement;\n\1"))
raiz("respuestas/cfe/ACKCFEdefType.java", "ACKCFEdefType", "ACKCFE")
raiz("respuestas/sobre/ACKSobredefType.java", "ACKSobredefType", "ACKSobre")
raiz("reporte/ReporteDefType.java", "ReporteDefType", "Reporte")

# 4. package-info: los de la app (prefijos DGICFE y firma, namespace por defecto): de ellos depende como sale el XML
for p in gen.rglob("package-info.java"):
    propio = actual / p.relative_to(gen)
    if propio.exists():
        p.write_text(propio.read_text(encoding="utf-8"), encoding="utf-8")
EOF

# A la salida, sin tocar el codigo escrito a mano (recepcion/wrappers)
for pkg in recepcion entreEmpresas reporte respuestas/cfe respuestas/sobre respuestas/reporte; do
	DEST="$SALIDA/dgi/classes/$pkg"
	mkdir -p "$DEST"
	find "$DEST" -maxdepth 1 -name "*.java" -delete
	cp "$GEN/dgi/classes/$pkg"/*.java "$DEST"/
done
echo "Clases generadas desde $XSDS en $SALIDA/dgi/classes"
