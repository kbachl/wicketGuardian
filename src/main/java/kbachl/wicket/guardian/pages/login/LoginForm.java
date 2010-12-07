package kbachl.wicket.guardian.pages.login;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;

/**
 * A very simple login form with two fields: {@code username} and
 * {@code password} (your HTML must have those two {@code wicket:id}s).
 * <p/>
 * Upon successful login, the user will be sent along to her original
 * destination (i.e. the page she requested before be intercepted by the
 * login page), or otherwise the home page.
 * <p/>
 * If login was unsuccessful, an error feedback message will be attached
 * to the username field: "Invalid username and/or password." You should
 * therefore ensure that there is a
 * {@link org.apache.wicket.markup.html.panel.FeedbackPanel FeedbackPanel}
 * present on your login page.
 * <p/>
 * This class is intended to get you started quickly with Shiro. If you need
 * anything more complicated, such as more nuanced error handling or localized
 * error messages, consider writing your own form using this code as a starting
 * point.
 *
 * @author Matt Brictson
 * @author Korbinian Bachl
 */
public class LoginForm extends StatelessForm<Void> {
    protected TextField<String> _usernameField;
    protected PasswordTextField _passwordField;

    /**
     * Creates a login form that does not have a {@code rememberme} checkbox.
     *
     * @param id component Id
     */
    public LoginForm(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(_usernameField = new RequiredTextField<String>(
                "username",
                new Model<String>()
        ));
        add(_passwordField = new PasswordTextField(
                "password",
                new Model<String>()
        ));
        add(new Button("submit", new ResourceModel("button")));
    }

    /**
     * Attempt authentication, and if successful continue to the user's
     * original destination or to the home page.
     */
    @Override
    protected void onSubmit() {
        String username = _usernameField.getModelObject();
        String password = _passwordField.getModelObject();

        // Just to be safe: make sure password doesn't remain in memory
        _passwordField.setModelObject(null);

        if (login(username, password, remember())) {
            if (!continueToOriginalDestination()) {
                setResponsePage(getApplication().getHomePage());
            }
        }
    }

    /**
     * TODO: change from String to String[] or char[] because in Java Strings are NOT secure
     * <p/>
     * Peform the actual authentication using Shiro's
     * {@link org.apache.shiro.subject.Subject#login login()} and handle any exceptions that are
     * thrown upon failure by setting an appropriate feedback message.
     *
     * @param username plain username
     * @param password plain password
     * @param remember plain remember
     * @return {@code true} if authentication succeeded
     */
    protected boolean login(String username, String password, boolean remember) {
        Subject currentUser = SecurityUtils.getSubject();
        UsernamePasswordToken token;
        token = new UsernamePasswordToken(username, password, remember);
        try {
            currentUser.login(token);
            return true;
        }
        catch (AuthenticationException ae) {
            _usernameField.error("Invalid username and/or password.");
        }
        return false;
    }

    /**
     * Override this method to return {@code true} if you want to enable
     * Shiro's "remember me" feature. By default this returns {@code false}.
     *
     * @return if should be remembered
     */
    protected boolean remember() {
        return false;
    }
}
