package gr.myapp.model;

import java.util.Date;

import javax.annotation.Resource;

import javax.enterprise.context.RequestScoped;

import javax.inject.Named;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import javax.naming.NamingException;

@Named
@RequestScoped
@SuppressWarnings("oracle.jdeveloper.cdi.not-proxyable-bean")
public class EmailSessionBean {

//    @Resource(lookup = "jndi/MyAppNoReplyMailSession")
//    private Session mailSession;

    public boolean sendEmail(String to, String subject, String body) {
        javax.naming.InitialContext ctx;
        try {
            ctx = new javax.naming.InitialContext();

            javax.mail.Session mailSession =
                (javax.mail.Session) ctx.lookup("mail/MyAppNoReplyMailSession");
            
            MimeMessage message = new MimeMessage(mailSession);

            message.setFrom(new InternetAddress("noreply@my-app.gr"));
            InternetAddress[] address = { new InternetAddress(to) };
            message.setRecipients(Message.RecipientType.TO, address);
            message.setSubject(subject);
            message.setSentDate(new Date());
            message.setText(body);
            
            Transport.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        } catch (NamingException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
