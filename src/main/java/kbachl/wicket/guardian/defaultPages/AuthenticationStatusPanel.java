package kbachl.wicket.guardian.defaultPages;

import org.apache.shiro.SecurityUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;

/**
 * Displays a login or logout link based on whether the current user is
 * known. If the user is known, also display the username. Consider placing
 * this panel on your base page.
 * <p/>
 * This class is intended to get you started quickly with Shiro.
 * Any non-trivial application should probably reimplement this panel with an
 * appropriate look and feel.
 *
 * @author Matt Brictson
 */
public class AuthenticationStatusPanel extends Panel {
    public AuthenticationStatusPanel(String id) {
        super(id);
        add(new Label("user", new PropertyModel<Object>(this, "username")));
        add(new LoginLink("login"));
        add(new LogoutLink("logout"));
    }

    /**
     * Returns the current Shiro principal (in other words, username).
     * Will be {@code null} if the user is unknown. You may override this if
     * the principal is not appropriate for display
     * (e.g. if it is a database identifier).
     *
     * @return username as String
     */
    public String getUsername() {
        return SecurityUtils.getSubject().getPrincipal().toString();
    }
}