package kbachl.wicket.guardian.panels;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresUser;
import org.apache.wicket.Page;
import org.apache.wicket.markup.html.link.Link;

/**
 * Logs out the current user and optionally redirects to another page.
 * The current Wicket session will be invalidated. This link is only visible
 * to authenticated/remembered users.
 *
 * @author Matt Brictson
 * @author Korbinian Bachl
 */
@RequiresUser
public class LogoutLink extends Link {
    private final Class<? extends Page> _destination;

    /**
     * Construct a logout link that will cause the user to remain on the
     * current page after the logout operation is performed.
     *
     * @param id Component Id
     */
    public LogoutLink(String id) {
        this(id, null);
    }

    /**
     * Construct a logout link that will cause the user to be redirected to
     * the specified page after the logout operation is performed.
     *
     * @param id              Component Id
     * @param destinationPage Class of destination Page
     */
    public LogoutLink(String id, Class<? extends Page> destinationPage) {
        super(id);
        _destination = destinationPage;
    }

    /**
     * Perform Shiro's
     * {@link org.apache.shiro.subject.Subject#logout logout()}
     * and redirect to the destination page, if one was specified.
     */
    public void onClick() {
        // Note that the default web implementation of Shiro's logout()
        // also invalidates the HTTP session.
        SecurityUtils.getSubject().logout();

        // If a post-logout page was specified, go there.
        if (_destination != null) {
            setResponsePage(_destination);
        }
        // Otherwise stay on the current page, unless we're no longer allowed
        // to see the current page now that we're logged out. In that case,
        // go to the home page.
        else if (!getPage().isActionAuthorized(RENDER)) {
            setResponsePage(getApplication().getHomePage());
        }
        setRedirect(true);
    }
}
