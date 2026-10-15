package com.bluedot.efactura.commons;

import java.util.Arrays;
import java.util.Collection;

import org.hibernate.Criteria;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.criterion.Order;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.play4jpa.jpa.models.DefaultQuery;

import play.mvc.Http;

/**
 * Orden de un listado paginado pedido por el panel: ?orden=campo&sentido=asc|desc. El campo es el nombre que usa el panel
 * (la columna); cada listado dice que campos admite y a que propiedades corresponden. Sin orden se usa el de siempre del
 * listado.
 */
public class Orden {

	public final String campo;

	public final boolean ascendente;

	private Orden(String campo, boolean ascendente) {
		this.campo = campo;
		this.ascendente = ascendente;
	}

	/**
	 * Lee el orden del request.
	 *
	 * @param permitidos
	 *            campos por los que se puede ordenar el listado
	 * @return null si no se pidio orden
	 * @throws APIException
	 *             BAD_PARAMETER_VALUE si el campo no se admite o el sentido no es asc ni desc
	 */
	public static Orden leer(Http.Request request, String... permitidos) throws APIException {
		return leer(request.getQueryString("orden"), request.getQueryString("sentido"), Arrays.asList(permitidos));
	}

	static Orden leer(String campo, String sentido, Collection<String> permitidos) throws APIException {
		if (campo == null || campo.trim().isEmpty())
			return null;
		campo = campo.trim();
		if (!permitidos.contains(campo))
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("orden", campo)
					.setDetailMessage("No se puede ordenar por " + campo + "; se admite: " + String.join(", ", permitidos));
		boolean ascendente = true;
		if (sentido != null && !sentido.trim().isEmpty()) {
			if (sentido.trim().equalsIgnoreCase("desc"))
				ascendente = false;
			else if (!sentido.trim().equalsIgnoreCase("asc"))
				throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("sentido", sentido)
						.setDetailMessage("sentido tiene que ser asc o desc");
		}
		return new Orden(campo, ascendente);
	}

	/**
	 * Ordena la consulta por estas propiedades (en ese orden), todas en el sentido pedido, y al final por id para que el
	 * orden sea estable entre paginas
	 */
	public void aplicar(DefaultQuery<?> q, String... propiedades) {
		// Directo en el Criteria: orderByAsc/Desc de play4jpa no admite alias creados con createAlias (ej. contraparte.razon)
		for (String propiedad : propiedades)
			q.getCriteria().addOrder(ascendente ? Order.asc(propiedad) : Order.desc(propiedad));
		q.getCriteria().addOrder(ascendente ? Order.asc("id") : Order.desc("id"));
	}

	/**
	 * Ordena por expresiones SQL en el sentido pedido (ej. subconsultas sobre filas relacionadas) y despues, para
	 * desempatar, por estas propiedades de la mas nueva a la mas vieja y por id. En las expresiones, {id} es la columna id
	 * de la entidad de la consulta.
	 */
	public void aplicarSql(DefaultQuery<?> q, String[] expresiones, String... desempateDescendente) {
		for (String sql : expresiones)
			q.getCriteria().addOrder(new OrderSql(sql, ascendente));
		for (String propiedad : desempateDescendente)
			q.getCriteria().addOrder(Order.desc(propiedad));
		q.getCriteria().addOrder(Order.desc("id"));
	}

	/** Order de Criteria con una expresion SQL */
	private static class OrderSql extends Order {

		private static final long serialVersionUID = 1L;

		private final String sql;

		OrderSql(String sql, boolean ascendente) {
			super("id", ascendente);
			this.sql = sql;
		}

		@Override
		public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) {
			return sql.replace("{id}", criteriaQuery.getColumns("id", criteria)[0]) + (isAscending() ? " asc" : " desc");
		}
	}
}
