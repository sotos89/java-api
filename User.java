package gr.myapp.model;

import java.util.Map;

public class User {
    
    String name;
    String password;
    String email;
    String firstname;
    String surname;
    String dateOfBirth;
    String nationality;
    String phone;
    
    String[] admin_channels;
    String[] admin_roles;
    
    String[] channels;
    
    String resetPasswordUuid;
    String validationEmailUuid;
    
    boolean disabled = false;
    
    String _id;
    String _rev;
    
    public User() {
        super();
    }
}
