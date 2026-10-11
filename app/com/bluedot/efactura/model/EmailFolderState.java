package com.bluedot.efactura.model;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

import org.hibernate.criterion.Restrictions;

import com.play4jpa.jpa.models.DefaultQuery;
import com.play4jpa.jpa.models.Finder;
import com.play4jpa.jpa.models.Model;

/**
 * Estado de sincronizacion IMAP de una carpeta del buzon de recepcion de una Empresa.
 *
 * Se guarda el ultimo UID procesado de cada carpeta. Los UID de IMAP no cambian cuando se
 * borran o mueven mensajes (a diferencia del numero de secuencia), mientras el UIDVALIDITY
 * de la carpeta no cambie.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "empresa_id", "folderName" }))
public class EmailFolderState extends Model<EmailFolderState> {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "email_folder_state_seq")
	@SequenceGenerator(name = "email_folder_state_seq", sequenceName = "email_folder_state_seq")
	private long id;

	@ManyToOne
	private Empresa empresa;

	private String folderName;

	private long uidValidity;

	private long lastUid;

	@Temporal(TemporalType.TIMESTAMP)
	private Date ultimaSincronizacion;

	public EmailFolderState() {
	}

	public EmailFolderState(Empresa empresa, String folderName) {
		this.empresa = empresa;
		this.folderName = folderName;
	}

	private static Finder<Long, EmailFolderState> find = new Finder<Long, EmailFolderState>(Long.class,
			EmailFolderState.class);

	public static EmailFolderState findByEmpresaYCarpeta(Empresa empresa, String folderName) {
		DefaultQuery<EmailFolderState> q = (DefaultQuery<EmailFolderState>) find.query();

		q.getCriteria().add(Restrictions.eq("empresa", empresa));
		q.getCriteria().add(Restrictions.eq("folderName", folderName));

		return q.findUnique();
	}

	public long getId() {
		return id;
	}

	public Empresa getEmpresa() {
		return empresa;
	}

	public void setEmpresa(Empresa empresa) {
		this.empresa = empresa;
	}

	public String getFolderName() {
		return folderName;
	}

	public void setFolderName(String folderName) {
		this.folderName = folderName;
	}

	public long getUidValidity() {
		return uidValidity;
	}

	public void setUidValidity(long uidValidity) {
		this.uidValidity = uidValidity;
	}

	public long getLastUid() {
		return lastUid;
	}

	public void setLastUid(long lastUid) {
		this.lastUid = lastUid;
	}

	public Date getUltimaSincronizacion() {
		return ultimaSincronizacion;
	}

	public void setUltimaSincronizacion(Date ultimaSincronizacion) {
		this.ultimaSincronizacion = ultimaSincronizacion;
	}

}
