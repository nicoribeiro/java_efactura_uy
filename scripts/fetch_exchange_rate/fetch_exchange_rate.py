import mysql.connector
from suds.client import Client
from suds.sudsobject import asdict
from datetime import datetime, date
import configparser
import os
import logging
import sys

# Set up logging for suds to capture SOAP requests
logging.basicConfig(level=logging.INFO)
logging.getLogger('suds.client').setLevel(logging.DEBUG)
logging.getLogger('suds.transport').setLevel(logging.DEBUG)

# Function to print nested object structure for debugging
def print_suds_object(obj, indent=0):
    if isinstance(obj, (list, tuple)):
        print("  " * indent + "[")
        for item in obj:
            print_suds_object(item, indent + 1)
        print("  " * indent + "]")
    elif hasattr(obj, '__dict__'):
        print("  " * indent + "{")
        for key, value in asdict(obj).items():
            print("  " * indent + f"  {key}: ", end="")
            print_suds_object(value, indent + 1)
        print("  " * indent + "}")
    else:
        print("  " * indent + str(obj))

# Function to normalize and validate date to YYYY-MM-DD
def normalize_date(date_input):
    try:
        if isinstance(date_input, date):  # Handle datetime.date
            return date_input.strftime('%Y-%m-%d')
        elif isinstance(date_input, str):  # Handle string
            parsed_date = datetime.strptime(date_input.strip(), '%Y-%m-%d')
            return parsed_date.strftime('%Y-%m-%d')
        else:
            raise ValueError(f"Unsupported date type: {type(date_input)}")
    except ValueError as e:
        print(f"Error normalizing date '{date_input}': {e}")
        print("Expected format: YYYY-MM-DD")
        return None

# Step 1: Read MariaDB connection parameters from config.ini
# Look for config.ini in the same directory as the script
script_dir = os.path.dirname(os.path.abspath(__file__))
config_file = os.path.join(script_dir, 'config.ini')
print(f"Current working directory: {os.getcwd()}")
print(f"Checking for config file at: {config_file}")
if not os.path.exists(config_file):
    print(f"Error: Configuration file {config_file} not found")
    exit(1)

config = configparser.ConfigParser()
try:
    config.read(config_file)
    db_config = {
        'host': config.get('Database', 'host'),
        'user': config.get('Database', 'user'),
        'password': config.get('Database', 'password'),
        'database': config.get('Database', 'database')
    }
except (configparser.NoSectionError, configparser.NoOptionError) as e:
    print(f"Error reading {config_file}: {e}")
    print("Ensure config.ini has a [Database] section with host, user, password, and database")
    exit(1)

# Step 2: Get the date from command-line argument or awsultimocierre
fecha = None
if len(sys.argv) > 1:
    input_date = sys.argv[1]
    print(f"Command-line date provided: {input_date}")
    fecha = normalize_date(input_date)
    if fecha is None:
        print("Invalid date format provided; falling back to awsultimocierre")
    else:
        print(f"Using command-line date: {fecha}")

if fecha is None:
    # Call awsultimocierre to get the latest closing date
    ultimo_cierre_wsdl = 'https://cotizaciones.bcu.gub.uy/wscotizaciones/servlet/awsultimocierre?wsdl'
    try:
        ultimo_cierre_client = Client(ultimo_cierre_wsdl)
        ultimo_cierre_result = ultimo_cierre_client.service.Execute()
        print("awsultimocierre response structure:")
        print_suds_object(ultimo_cierre_result)
        
        # Access Fecha directly
        try:
            fecha_raw = ultimo_cierre_result.Fecha
            print(f"Raw Fecha value: '{fecha_raw}' (type: {type(fecha_raw)})")
            # Normalize the date
            fecha = normalize_date(fecha_raw)
            print(f"Normalized closing date: {fecha}")
        except AttributeError as e:
            print(f"Error accessing Fecha: {e}")
            print("Please inspect the response structure above to find the correct attribute for Fecha")
            exit(1)
    except Exception as e:
        print(f"Error calling awsultimocierre: {e}")
        exit(1)

if fecha is None:
    print("No valid date obtained from command-line or awsultimocierre")
    exit(1)

# Step 3: Call awsbcucotizaciones with the normalized date
cotizaciones_wsdl = 'https://cotizaciones.bcu.gub.uy/wscotizaciones/servlet/awsbcucotizaciones?wsdl'
try:
    cotizaciones_client = Client(cotizaciones_wsdl)
    cotiza_obj = cotizaciones_client.factory.create("wsbcucotizacionesin")
    cotiza_obj.FechaDesde = fecha
    cotiza_obj.FechaHasta = fecha
    cotiza_obj.Grupo = 2
    cotiza_obj.Moneda = {'item': 2225}  # USD
    print(f"Calling awsbcucotizaciones with FechaDesde={fecha}, FechaHasta={fecha}, Grupo=2, Moneda=2225")
    cotizaciones_result = cotizaciones_client.service.Execute(Entrada=cotiza_obj)
    
    # Print raw response for debugging
    print("Raw cotizaciones_result:", cotizaciones_result)
    # Print response structure for debugging
    print("awsbcucotizaciones response structure:")
    print_suds_object(cotizaciones_result)
    
    # Extract TCC from the result
    tcc = None
    if hasattr(cotizaciones_result, 'datoscotizaciones'):
        datoscotizaciones = cotizaciones_result.datoscotizaciones
        # Debug: Print all attributes of datoscotizaciones
        print("Attributes of datoscotizaciones:", [attr for attr in dir(datoscotizaciones) if not attr.startswith('_')])
        if hasattr(datoscotizaciones, 'datoscotizaciones.dato'):
            datos = getattr(datoscotizaciones, 'datoscotizaciones.dato')
            if not isinstance(datos, (list, tuple)):
                datos = [datos]  # Ensure iterable
            for dato in datos:
                print(f"Checking Moneda: '{dato.Moneda}' (type: {type(dato.Moneda)})")
                if str(dato.Moneda) == "2225":  # Compare as string
                    tcc = dato.TCC
                    print(f"TCC for USD on {fecha}: {tcc}")
                    break
        else:
            print("No 'datoscotizaciones.dato' attribute found in datoscotizaciones")
            exit(1)
    else:
        print("No 'datoscotizaciones' attribute found in cotizaciones_result")
        exit(1)
    
    if tcc is None:
        print("No TCC data found for USD")
        exit(1)
except Exception as e:
    print(f"Error calling awsbcucotizaciones: {e}")
    try:
        print("awsbcucotizaciones response structure (if available):")
        print_suds_object(cotizaciones_result)
    except NameError:
        print("No response available due to error")
    exit(1)

# Step 4: Insert into MariaDB if no row exists for the date
try:
    # Database connection using parameters from config
    conn = mysql.connector.connect(**db_config)
    cursor = conn.cursor()
    
    # Check if a row exists for the given fecha
    check_query = """
    SELECT COUNT(*) FROM tipo_de_cambio WHERE fecha = %s
    """
    cursor.execute(check_query, (fecha,))
    row_count = cursor.fetchone()[0]
    
    if row_count > 0:
        print(f"Row already exists for fecha={fecha}, skipping insertion")
    else:
        # Insert query
        insert_query = """
        INSERT INTO tipo_de_cambio (fecha, interbancario, moneda)
        VALUES (%s, %s, %s)
        """
        values = (fecha, tcc, 'USD')
        cursor.execute(insert_query, values)
        conn.commit()
        print(f"Inserted row into tipo_de_cambio: fecha={fecha}, interbancario={tcc}, moneda=USD")
    
except mysql.connector.Error as e:
    print(f"Database error: {e}")
    exit(1)
finally:
    if 'cursor' in locals():
        cursor.close()
    if 'conn' in locals():
        conn.close()