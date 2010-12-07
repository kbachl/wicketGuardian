package kbachl.wicket.guardian.pages.login;

import org.apache.shiro.SecurityUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.panel.FeedbackPanel;

/**
 * A basic login page that contains a login form and feedback panel.
 * <p/>
 * This class is intended to get you started quickly with Shiro.
 * Any non-trivial application should implement its own login page with an
 * appropriate look and feel.
 *
 * @author Matt Brictson
 */
public class LoginPage extends WebPage {
    public LoginPage() {
        super();
        add(CSSPackageResource.getHeaderContribution(LoginPage.class, "loginpage.css"));
        add(new RememberMeLoginForm("login"));
        add(new FeedbackPanel("feedback"));
    }

    /**
     * If the user is already authenticated, don't bother displaying the
     * login page; instead redirect to home page.
     */
    @Override
    protected void onBeforeRender() {
        if (SecurityUtils.getSubject().isAuthenticated()) {
            throw new RestartResponseException(getApplication().getHomePage());
        }
        super.onBeforeRender();
    }
}
