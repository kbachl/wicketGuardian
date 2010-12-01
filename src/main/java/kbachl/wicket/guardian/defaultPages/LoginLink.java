package kbachl.wicket.guardian.defaultPages;

import kbachl.wicket.guardian.WicketGuardian;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresGuest;
import org.apache.wicket.Page;
import org.apache.wicket.RestartResponseAtInterceptPageException;
import org.apache.wicket.markup.html.link.StatelessLink;

/**
 * A link that sends the user to the login page. Upon successful login, the
 * user will be returned to the page that contained the login link, or
 * to a pre-determined page. This link is not visible to
 * authenticated/remembered users.
 *
 * @author Matt Brictson
 * @author Korbinan Bachl
 */
@RequiresGuest
public class LoginLink extends StatelessLink {
    private Class<? extends Page> _destination;

    /**
     * Construct a login link that will cause the user to remain on the
     * current page after login has succeeded.
     *
     * @param id component Id
     */
    public LoginLink(String id) {
        this(id, null);
    }

    /**
     * Construct a login link that will cause the user to be redirected to
     * the specified page after login has succeeded.
     *
     * @param id              component Id
     * @param destinationPage the destination page
     */
    public LoginLink(String id, Class<? extends Page> destinationPage) {
        super(id);
        _destination = destinationPage;
    }

    /**
     * Throws {@link org.apache.wicket.RestartResponseAtInterceptPageException} to redirect
     * the user to the login page. If the user is already authenticated,
     * do nothing.
     */
    public void onClick() {
        // The way RestartResponseAtInterceptPageException works, after the
        // user successfully authenticates she will be redirected back to this
        // onClick() handler. This if-statement ensures that on the second
        // trip the user won't be sent to the login page again.
        if (!SecurityUtils.getSubject().isAuthenticated()) {
            throw new RestartResponseAtInterceptPageException(getLoginPage());
        }
        if (_destination != null) {
            setResponsePage(_destination);
        }
        setRedirect(true);
    }

    /**
     * Returns {@code true} if {@code Page} is the login page.
     */
    @Override
    protected boolean linksTo(Page page) {
        return page.getClass().equals(getLoginPage());
    }

    /**
     * Returns the login page that is configured for this application by
     * asking the {@link WicketGuardian}.
     *
     * @return the login page
     */
    protected Class<? extends Page> getLoginPage() {
        return WicketGuardian.get().getLoginPage();
    }
}