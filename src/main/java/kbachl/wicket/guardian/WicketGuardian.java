package kbachl.wicket.guardian;


import kbachl.wicket.guardian.pages.login.LoginPage;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.aop.*;
import org.apache.wicket.*;
import org.apache.wicket.authorization.Action;
import org.apache.wicket.authorization.IAuthorizationStrategy;
import org.apache.wicket.authorization.IUnauthorizedComponentInstantiationListener;
import org.apache.wicket.authorization.UnauthorizedInstantiationException;
import org.apache.wicket.markup.html.pages.AccessDeniedPage;
import org.apache.wicket.settings.ISecuritySettings;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * Enhances Wicket to integrate closely with the Apache Shiro security
 * framework. With the {@code WicketGuardian} installed in your Wicket
 * application, you will gain the following features:
 * <ul>
 * <li>You can use all of Shiro's authorization annotations
 * (like
 * {@link org.apache.shiro.authz.annotation.RequiresAuthentication @RequiresAuthentication}
 * and
 * {@link org.apache.shiro.authz.annotation.RequiresPermissions @RequiresPermissions})
 * on Wicket Pages. The {@code WicketGuardian} will ensure that only
 * authorized users can access these pages, and will show an appropriate
 * error page or login page otherwise.
 * See {@link #isInstantiationAuthorized isInstantiationAuthorized()}.
 * </li>
 * <li>You can also use the same Shiro annotations on individual components,
 * like Links and Panels. The {@code WicketGuardian} will automatically
 * hide these components from unauthorized users.
 * See {@link #isActionAuthorized isActionAuthorized()}.
 * </li>
 * <li>You can access Shiro directly at any time in your Wicket code
 * by calling
 * {@link org.apache.shiro.SecurityUtils#getSubject SecurityUtils.getSubject()}.
 * This gives you access to the rich set of security operations on the
 * Shiro {@link org.apache.shiro.subject.Subject Subject} that represents
 * the current user.
 * </li>
 * <li>Any uncaught Shiro
 * {@link org.apache.shiro.authz.AuthorizationException AuthorizationExceptions}
 * will be handled gracefully by redirecting the user to the
 * login page or an unauthorized error page. This allows you to implement
 * comprehensive security rules using Shiro at any tier of your
 * application and be confident that your UI will handle them
 * appropriately.
 * See {@link #onException onException()}.
 * </li>
 * </ul>
 * <h2>Installation</h2>
 * Before you can use the {@code WicketGuardian}, you must have Shiro
 * properly added to your application's {@code web.xml} file. Refer to the
 * Overview section of this Javadoc for a brief tutorial.
 * <h3>{@code Application.init()}</h3>
 * Once Shiro itself is installed, adding {@code WicketGuardian} can be as
 * simple as adding one line to your Wicket application {@code init()}:
 * <pre class="example">
 * public class MyApplication extends WebApplication
 * {
 * &#064;Override
 * protected void init()
 * {
 * super.init();
 * new WicketGuardian().install(this);
 * }
 * }</pre>
 * Most developers will want to customize the login page and error pages.
 * The more complex real-world installation is thus:
 * <pre class="example">
 * public class MyApplication extends WebApplication
 * {
 * &#064;Override
 * protected void init()
 * {
 * super.init();
 * new WicketGuardian()
 * .setLoginPage(MyLoginPage.class)
 * .setUnauthorizedPage(MyAccessDeniedPage.class)
 * .install(this);
 * }
 * }</pre>
 * <h3>{@code RequestCycle.onRuntimeException()}</h3>
 * Finally, if you plan on performing authorization checks in the backend
 * (that is, in addition to or instead of using annotations on your Wicket
 * pages), you will want to gracefully handle uncaught Shiro exceptions at
 * the Wicket layer. To do this, configure your Wicket application to use a
 * custom {@code RequestCycle} if you have not already done so:
 * <pre class="example">
 * public class MyApplication extends WebApplication
 * {
 * &#064;Override
 * public WicketRequestCycle newRequestCycle(Request request, Response response)
 * {
 * return new MyRequestCycle(this, (WebRequest) request, response);
 * }
 * }</pre>
 * Within your custom {@code RequestCycle} class, delegate to the
 * {@code WicketGuardian} to handle Shiro exceptions:
 * <pre class="example">
 * public class MyRequestCycle extends WebRequestCycle
 * {
 * &#064;Override
 * public Page onRuntimeException(Page page, RuntimeException e)
 * {
 * WicketGuardian.get().onException(page, e);
 * return super.onRuntimeException(page, e);
 * }
 * }</pre>
 *
 * @author Matt Brictson
 * @author Korbinian Bachl
 */
public class WicketGuardian
        implements IAuthorizationStrategy,
        IUnauthorizedComponentInstantiationListener {
    /**
     * The key that will be used to obtain a localized message
     * when access is denied due to the user be unauthenticated.
     */
    public static final String LOGIN_REQUIRED_MESSAGE_KEY = "loginRequired";

    private static final MetaDataKey<AuthorizationException> EXCEPTION_KEY =
            new MetaDataKey<AuthorizationException>() {
            };

    private static final AuthorizingAnnotationHandler[] HANDLERS =
            new AuthorizingAnnotationHandler[]{
                    new AuthenticatedAnnotationHandler(),
                    new GuestAnnotationHandler(),
                    new PermissionAnnotationHandler(),
                    new RoleAnnotationHandler(),
                    new UserAnnotationHandler()
            };

    /**
     * Returns the {@code WicketGuardian} instance that has been installed
     * in the current Wicket application. This is a convenience method that
     * only works within a Wicket thread, and it assumes that
     * {@link #install install()} has already been called.
     *
     * @return instance of WicketGuardian
     * @throws IllegalStateException if there is no Wicket application bound
     *                               to the current thread, or if a
     *                               {@code WicketGuardian} has not been
     *                               installed.
     */
    public static WicketGuardian get() {
        Application app = Application.get();
        if (null == app) {
            throw new IllegalStateException(
                    "No wicket application is bound to the current thread."
            );
        }
        ISecuritySettings settings = app.getSecuritySettings();
        IAuthorizationStrategy authz = settings.getAuthorizationStrategy();
        if (!(authz instanceof WicketGuardian)) {
            throw new IllegalStateException(
                    "A WicketGuardian has not been installed in this Wicket " +
                            "application. You must call WicketGuardian.install() in " +
                            "your application init()."
            );
        }
        return (WicketGuardian) authz;
    }


    private Class<? extends Page> loginPage = LoginPage.class;
    private Class<? extends Page> unauthorizedPage = AccessDeniedPage.class;

    public Class<? extends Page> getLoginPage() {
        return loginPage;
    }

    public WicketGuardian setLoginPage(Class<? extends Page> loginPage) {
        this.loginPage = loginPage;
        return this;
    }

    public Class<? extends Page> getUnauthorizedPage() {
        return unauthorizedPage;
    }

    public WicketGuardian setUnauthorizedPage(Class<? extends Page> page) {
        this.unauthorizedPage = page;
        return this;
    }

    /**
     * Installs this {@code WicketGuardian} as both the
     * {@link IAuthorizationStrategy} and the
     * {@code IUnauthorizedComponentInstantiationListener} for the given
     * Wicket application.
     *
     * @param app the applicaiton that should be guarded
     */
    public void install(Application app) {
        ISecuritySettings settings = app.getSecuritySettings();
        settings.setAuthorizationStrategy(this);
        settings.setUnauthorizedComponentInstantiationListener(this);
    }

    /**
     * Determine what caused the unauthorized instantiation of the given
     * component. If access was denied due to being unauthenticated, and
     * the login page specified in the constructor was not {@code null},
     * redirect to the login page. Place a localized error feedback message
     * in the Session using the key {@code loginRequired}.
     * <p/>
     * Otherwise, if the login page was {@code null} or access was denied
     * due to authorization failure (e.g. insufficient privileges), throw
     * an {@link org.apache.wicket.authorization.UnauthorizedInstantiationException} with the original
     * Shiro {@link AuthorizationException} as the cause.
     *
     * @param component The component that failed to initialize due to
     *                  authorization or authentication failure
     * @throws org.apache.wicket.authorization.UnauthorizedInstantiationException
     *          for unauthorized errors
     *          or if the login page is
     *          {@code null}
     */
    public void onUnauthorizedInstantiation(Component component) {
        AuthorizationException cause;
        cause = RequestCycle.get().getMetaData(EXCEPTION_KEY);

        // Show appropriate login or error page if possible
        onException(component, cause);

        // Otherwise bubble up the error
        UnauthorizedInstantiationException ex;
        ex = new UnauthorizedInstantiationException(component.getClass());
        ex.initCause(cause);
        throw ex;
    }

    /**
     * React to an uncaught Exception by redirecting the browser to
     * the unauthorized page or login page if appropriate. Application
     * developers should call this method within their subclass of
     * {@link RequestCycle#onRuntimeException RequestCycle.onRuntimeException()}
     * to allow uncaught Shiro exceptions thrown by the backend to be
     * handled gracefully by the Wicket layer.
     * <p/>
     * If the exception is a Shiro {@link AuthorizationException}, redirect
     * to the unauthorized page or login page depending on the type of error.
     * If the exception is not a Shiro {@link AuthorizationException}
     * return silently.
     *
     * @param component The Wicket Page or other component that was the source
     *                  of the error, if known. May be {@code null}.
     * @param error     The exception to handle. If it is not a subclass of
     *                  Shiro's {@link AuthorizationException}, this method will
     *                  not have any effect.
     * @throws RestartResponseAtInterceptPageException
     *                                  to redirect to the login
     *                                  page if the error is due to the user being
     *                                  <em>unauthenticated</em>.
     * @throws RestartResponseException to render the unauthorized page
     *                                  if the error is due to the user being
     *                                  <em>unauthorized</em>.
     */
    public void onException(Component component, Exception error) {
        if (error instanceof AuthorizationException) {
            AuthorizationException ae = (AuthorizationException) error;
            if (authenticationNeeded(ae)) {
                if (loginPage != null) {
                    Session.get().error(getLoginRequiredMessage(component));
                    throw new RestartResponseAtInterceptPageException(loginPage);
                }
            } else if (unauthorizedPage != null) {
                throw new RestartResponseException(unauthorizedPage);
            }
        }
    }

    /**
     * Performs authorization checks for the {@link Component#RENDER RENDER}
     * action only. Other actions are always allowed.
     * <p/>
     * If the action is {@code RENDER}, the component class <em>and its
     * superclasses</em> are checked for the presence of
     * {@link org.apache.shiro.authz.annotation Shiro annotations}.
     * <p/>
     * The absence of any Shiro annotation means that the component may be
     * rendered, and {@code true} is returned. Otherwise, each annotation is
     * evaluated against the current Shiro Subject. If any of the requirements
     * dictated by the annotations fail, {@code false} is returned and
     * rendering for the component will be skipped.
     * <p/>
     * For example, this link will be hidden if the user is already
     * authenticated:
     * <pre class="example">
     * &#064;RequiresGuest
     * public class LoginLink extends StatelessLink
     * {
     * ...
     * }</pre>
     */
    public boolean isActionAuthorized(Component component, Action action) {
        if (Component.RENDER.equals(action)) {
            try {
                assertAuthorized(component.getClass());
            }
            catch (AuthorizationException ae) {
                return false;
            }
        }
        return true;
    }

    /**
     * If {@code componentClass} is a subclass of {@link Page},
     * return {@code true} or {@code false} based on evaluation of any
     * {@link org.apache.shiro.authz.annotation Shiro annotations}
     * that are present on the page class declaration, <em>plus any annotations
     * present on its superclasses</em>.
     * <p/>
     * The absence of any Shiro annotation means that the page can always be
     * instantiated, meaning {@code true} will always be returned. Otherwise,
     * each annotation is evaluated against the current Shiro Subject. If any
     * of the requirements dictated by the annotations fail, {@code false} will
     * be returned and instantiation will be denied.
     * <p/>
     * For example, this page may only be instantiated if the user has
     * explictly authenticated (i.e. not just "remembered" via cookie) and
     * additionally has the "admin" role:
     * <pre class="example">
     * &#064;RequiresAuthentication
     * &#064;RequiresRoles("admin")
     * public class TopSecretPage extends WebPage
     * {
     * ...
     * }</pre>
     * If {@code componentClass} is not a subclass of Page, always return
     * {@code true}. Non-page components may always be instantiated; however
     * their rendering can be controlled via annotations. See
     * {@link #isActionAuthorized isActionAuthorized()}.
     */
    public <T extends Component> boolean isInstantiationAuthorized(Class<T> componentClass) {

        if (Page.class.isAssignableFrom(componentClass)) {
            try {
                assertAuthorized(componentClass);
            }
            catch (AuthorizationException ae) {
                // Store exception for use later in the request by
                // ShiroUnauthorizedComponentInstantiationListener
                RequestCycle.get().setMetaData(EXCEPTION_KEY, ae);
                return false;
            }
        }
        return true;
    }

    /**
     * Simple method based on isInstantiationAuthorized now for all Components
     * TODO: clearify why isInstantiationAuthorized is limited to Page-classes only?
     *
     * @param componentClass
     * @param <T>
     * @return
     */
    public <T extends Component> boolean isComponentInstantiationAuthorized(Class<T> componentClass) {

        try {
            assertAuthorized(componentClass);
        }
        catch (AuthorizationException ae) {
            // Store exception for use later in the request by
            // ShiroUnauthorizedComponentInstantiationListener
            RequestCycle.get().setMetaData(EXCEPTION_KEY, ae);
            return false;
        }
        return true;
    }

    /**
     * Convinient method to do a simple check against a class
     *
     * @param componentClass class of the component/ page
     * @param <T>            ext. Component
     * @return true if isAuthorized
     */
    public static <T extends Component> boolean isAuthorizedFor(Class<T> componentClass) {
        return get().isComponentInstantiationAuthorized(componentClass);
    }

    /**
     * Convinient method to do a simple check against a class
     *
     * @param c class of the component/ page
     * @return true if isAuthorized
     */
    public static boolean isAuthorizedFor(Component c) {
        return get().isComponentInstantiationAuthorized(c.getClass());
    }


    /**
     * Returns the localized message for the {@code loginRequired} key.
     *
     * @param component Component
     * @return a message String
     */
    private String getLoginRequiredMessage(Component component) {

        return component.getLocalizer().getString(
                LOGIN_REQUIRED_MESSAGE_KEY,
                component,
                null,
                ResourceBundle.getBundle(this.getClass().getName()).getString("LOGIN_REQUIRED_MESSAGE")
        );
    }

    /**
     * Returns {@code true} if the reason the user was denied access is
     * because she needs to authenticate.
     *
     * @param cause the cause of the AuthorizationException
     * @return is authentication needed?
     */
    private boolean authenticationNeeded(AuthorizationException cause) {
        boolean needLogin = false;

        // Check if Shiro blocked access due to authentication
        if (cause instanceof UnauthenticatedException) {
            needLogin = true;

            // But... there is a rare case where Shiro can throw an
            // UnauthenticatedException even when the user is already logged
            // in. If the user is logged in and the page was annotated with
            // @RequiresGuest, Shiro throws an UnauthenticatedException, which
            // which is very misleading. Our only way to detect this scenario
            // is to parse the exception message. Yes, this is a hack.

            String msg = cause.getMessage();
            String guestError = "Attempting to perform a guest-only operation.";
            if (msg != null && msg.startsWith(guestError)) {
                needLogin = false;
            }
        }
        return needLogin;
    }

    /**
     * @param cls Class
     * @throws AuthorizationException if the given class, or any of its
     *                                superclasses, has a Shiro annotation that fails its
     *                                authorization check.
     */
    private void assertAuthorized(final Class<?> cls)
            throws AuthorizationException {
        Collection<Annotation> annotations = findAnnotations(cls);
        for (Annotation annot : annotations) {
            for (AuthorizingAnnotationHandler h : HANDLERS) {
                h.assertAuthorized(annot);
            }
        }
    }

    /**
     * Returns all annotations present on the given class and all of its
     * superclasses.
     *
     * @param cls Class for inspection
     * @return returns a Collection of Annotations
     */
    private Collection<Annotation> findAnnotations(final Class<?> cls) {
        List<Annotation> annots = new ArrayList<Annotation>(5);
        Class<?> currClass = cls;
        while (currClass != null) {
            annots.addAll(Arrays.asList(currClass.getDeclaredAnnotations()));
            currClass = currClass.getSuperclass();
        }
        return annots;
    }
}
