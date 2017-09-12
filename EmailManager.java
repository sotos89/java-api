package gr.myapp.model;

//import java.util.Properties;
//
//import javax.mail.Message;
//import javax.mail.MessagingException;
//import javax.mail.PasswordAuthentication;
//import javax.mail.Session;
//import javax.mail.Transport;
//import javax.mail.internet.InternetAddress;
//import javax.mail.internet.MimeMessage;

public class EmailManager {

    public boolean sendEmail(String to, String resetPasswordUuid, String username) {
        EmailSessionBean emsb = new EmailSessionBean();
        String link = "http://www.my-app.gr/forgot/reset.php?name=" + username + "&token=" + resetPasswordUuid;
        return emsb.sendEmail(to, "Reset password", "Reset your password by copying this url to your browser: " + link);
    }
    
    public boolean sendValidateEmail(String to, String validateEmailUuid , String username) {
        EmailSessionBean emsb = new EmailSessionBean();
        String link = "http://www.my-app.gr/forgot/validate_email.php?name=" + username + "&token=" + validateEmailUuid;
        return emsb.sendEmail(to, "Validate email", "Validate your email by copying this url to your browser: " + link);
    }
}
