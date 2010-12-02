package kbachl.wicket.guardian.defaultPages.login;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.Model;

/**
 * A variation of {@link kbachl.wicket.guardian.defaultPages.login.LoginForm} that includes a "remember me" checkbox.
 * The checkbox will be unchecked by default. Your markup must contain
 * {@code <input type="checkbox" wicket:id="rememberme"/>}.
 *
 * @author Matt Brictson
 * @author Korbinian Bachl
 */
public class RememberMeLoginForm extends LoginForm {
    private CheckBox _rememberCheck;

    /**
     * Create a login form with a "remember me" checkbox.
     *
     * @param id Component Id
     */
    public RememberMeLoginForm(String id) {
        super(id);
        add(_rememberCheck = new CheckBox("rememberme", Model.of(false)));
    }

    /**
     * Returns {@code true} to enable the "remember me" feature if the
     * checkbox is checked.
     */
    protected boolean remember() {
        return _rememberCheck.getModelObject();
    }
}
