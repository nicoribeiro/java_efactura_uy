package db;

import org.hibernate.cfg.ImprovedNamingStrategy;

import com.bluedot.commons.utils.Inflector;

public class CustomNamingStrategy extends ImprovedNamingStrategy {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1982371283991374326L;

	@Override
	public String foreignKeyColumnName(String propertyName,
			String propertyEntityName, String propertyTableName,
			String referencedColumnName) {

		//Logger.info(propertyName +" -- "+ propertyEntityName +" -- "+ propertyTableName +" -- "+ referencedColumnName);
		
		if (propertyName==null)
			return (propertyTableName+"_"+referencedColumnName).toLowerCase();
		else
			return (Inflector.getInstance().singularize(propertyName)+"_"+referencedColumnName).toLowerCase();
		
	}
	
	@Override
	public String collectionTableName(String ownerEntity,
			String ownerEntityTable, String associatedEntity,
			String associatedEntityTable, String propertyName) {
		
		//Logger.info(ownerEntity +" -- "+ ownerEntityTable +" -- "+ associatedEntity +" -- "+ associatedEntityTable +" -- " + propertyName);
		
		return super.collectionTableName(ownerEntity, ownerEntityTable,
				associatedEntity, associatedEntityTable, Inflector.getInstance().singularize(propertyName));
	}

}
