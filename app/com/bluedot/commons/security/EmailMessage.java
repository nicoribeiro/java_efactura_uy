package com.bluedot.commons.security;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;

import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.utils.Tuple;
import com.bluedot.efactura.model.Empresa;
import com.play4jpa.jpa.models.DefaultQuery;
import com.play4jpa.jpa.models.Finder;
import com.play4jpa.jpa.models.Model;

@Entity
public class EmailMessage extends Model<EmailMessage> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8701151304733591795L;

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO,generator="email_message_seq")
	@SequenceGenerator(name = "email_message_seq", sequenceName = "email_message_seq" )
	private long id;

	private String messageId;
	private String fromEmail;
	private String subject;
	private Date sentDate;
	private String messageContent;
	
	@OneToOne
	private Empresa empresa;

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "emailMessage", fetch=FetchType.LAZY)
	private List<Attachment> attachments;

	public EmailMessage() {

	}

	public EmailMessage(com.bluedot.commons.utils.messaging.Email email) {
		this.messageId = email.getMessageId();
		this.fromEmail = email.getFrom();
		this.subject = email.getSubject();
		this.sentDate = email.getSentDate();
		this.messageContent = email.getMessageContent(); // TODO esto es siempre vacio
		for (com.bluedot.commons.utils.messaging.Attachment attachmentRaw : email.getAttachments()) {
			Attachment attachment = new Attachment(attachmentRaw);
			attachment.setEmailMessage(this);
			attachment.save();
			getAttachments().add(attachment);
		}

	}

	public EmailMessage(String messageId, String from, String subject, Date sentDate, String messageContent) {
		super();
		this.messageId = messageId;
		this.fromEmail = from;
		this.subject = subject;
		this.sentDate = sentDate;
		this.messageContent = messageContent;
	}

	private static Finder<Long, EmailMessage> find = new Finder<Long, EmailMessage>(Long.class, EmailMessage.class);
	
	public static EmailMessage findById(Long id) {
		return find.byId(id);
	}
	
	public static EmailMessage findById(Long id, boolean throwExceptionWhenMissing) throws APIException {
		EmailMessage emailMessage = find.byId(id);

		if (emailMessage == null && throwExceptionWhenMissing)
			throw APIException.raise(APIErrors.NO_EXISTE_EMAIL).withParams("id", id);
		return emailMessage;
	}
	
	
	/**
	 * Emails recibidos por una empresa, del mas nuevo al mas viejo.
	 * 
	 * @param texto
	 *            busca en asunto y remitente (contiene, sin distinguir mayusculas); null = sin filtro
	 * @param estado
	 *            emails con al menos un adjunto en ese estado; null = sin filtro
	 * @param desde
	 *            fecha de envio mayor o igual; null = sin filtro
	 * @param hasta
	 *            fecha de envio menor o igual; null = sin filtro
	 */
	public static Tuple<List<EmailMessage>, Long> find(Empresa empresa, String texto, AttachmentEstado estado, Date desde,
			Date hasta, int page, int pageSize) {
		DefaultQuery<EmailMessage> q = (DefaultQuery<EmailMessage>) find.query();

		q.getCriteria().add(Restrictions.eq("empresa", empresa));

		if (texto != null && !texto.trim().isEmpty()) {
			String like = "%" + texto.trim() + "%";
			q.getCriteria().add(Restrictions.or(Restrictions.ilike("subject", like), Restrictions.ilike("fromEmail", like)));
		}

		if (estado != null) {
			DetachedCriteria conAdjuntoEnEstado = DetachedCriteria.forClass(Attachment.class, "adjunto")
					.add(Restrictions.eq("adjunto.estado", estado))
					.setProjection(Projections.property("adjunto.emailMessage.id"));
			q.getCriteria().add(Subqueries.propertyIn("id", conAdjuntoEnEstado));
		}

		if (desde != null)
			q.getCriteria().add(Restrictions.ge("sentDate", desde));
		if (hasta != null)
			q.getCriteria().add(Restrictions.le("sentDate", hasta));

		long rowCount = q.findRowCount();

		q.orderByDesc("sentDate");
		q.orderByDesc("id");

		return new Tuple<List<EmailMessage>, Long>(q.findPage(page, pageSize), rowCount);
	}

	public static List<EmailMessage> findByMessageId(String messageId) {

		DefaultQuery<EmailMessage> q = (DefaultQuery<EmailMessage>) find.query();

		q.getCriteria().add(Restrictions.eq("messageId", messageId));

		return q.findList();

	}

	public static List<EmailMessage> findByMessageId(String messageId, Empresa empresa) {

		DefaultQuery<EmailMessage> q = (DefaultQuery<EmailMessage>) find.query();

		q.getCriteria().add(Restrictions.eq("messageId", messageId));
		q.getCriteria().add(Restrictions.eq("empresa", empresa));

		return q.findList();

	}

	public String getFromEmail() {
		return fromEmail;
	}

	public void setFromEmail(String fromEmail) {
		this.fromEmail = fromEmail;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public Date getSentDate() {
		return sentDate;
	}

	public void setSentDate(Date sentDate) {
		this.sentDate = sentDate;
	}

	public String getMessageContent() {
		return messageContent;
	}

	public void setMessageContent(String messageContent) {
		this.messageContent = messageContent;
	}

	public List<Attachment> getAttachments() {
		if (attachments == null)
			attachments = new LinkedList<Attachment>();
		return attachments;
	}

	public void setAttachments(List<Attachment> attachments) {
		this.attachments = attachments;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Empresa getEmpresa() {
		return empresa;
	}

	public void setEmpresa(Empresa empresa) {
		this.empresa = empresa;
	}
}
