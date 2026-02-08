package org.tavall.couriers.api.web.endpoints;
import org.tavall.couriers.api.web.endpoints.camera.CameraFeedEndpoints;
import org.tavall.couriers.api.web.endpoints.config.ClientConfigEndpoints;
import org.tavall.couriers.api.web.endpoints.dashboard.DefaultDashboardEndpoints;
import org.tavall.couriers.api.web.endpoints.dashboard.driver.DriverDashboardEndpoints;
import org.tavall.couriers.api.web.endpoints.dashboard.merchant.MerchantDashboardEndpoints;
import org.tavall.couriers.api.web.endpoints.dashboard.superuser.SuperUserDashboardEndpoints;
import org.tavall.couriers.api.web.endpoints.page.PageViewEndpoints;
import org.tavall.couriers.api.web.endpoints.page.track.TrackingEndpoints;
import org.tavall.couriers.api.web.endpoints.shipping.ShippingLabelEndpoints;
import org.tavall.couriers.api.web.endpoints.user.UserAuthEndpoints;

// Centralized routes helper for controllers and Thymeleaf templates.
public final class Routes {

    private Routes() {}

    // Page views
    public static final String HOME = PageViewEndpoints.HOME_PATH;
    public static final String LOGIN = PageViewEndpoints.LOGIN_PATH;
    public static final String TRACKING_PAGE = PageViewEndpoints.TRACKING_PATH;
    public static final String TRACKING_DETAIL_TEMPLATE = PageViewEndpoints.TRACKING_DETAIL_PATH;

    // Dashboard views
    public static final String DASHBOARD = DefaultDashboardEndpoints.DASHBOARD_HOME_PATH;
    public static final String DASHBOARD_HOME_ALIAS = DefaultDashboardEndpoints.DASHBOARD_HOME_ALIAS_PATH;
    public static final String DASHBOARD_LOGIN_HOME = DefaultDashboardEndpoints.DASHBOARD_LOGIN_HOME_PATH;
    public static final String DASHBOARD_LOGOUT = DefaultDashboardEndpoints.DASHBOARD_LOGOUT_PATH;
    public static final String DASHBOARD_ACCESS_DENIED = DefaultDashboardEndpoints.DASHBOARD_ACCESS_DENIED_PATH;
    public static final String DASHBOARD_ERROR = DefaultDashboardEndpoints.DASHBOARD_ERROR_PATH;
    public static final String DASHBOARD_STATUS = DefaultDashboardEndpoints.DASHBOARD_STATUS_PATH;
    public static final String DASHBOARD_ADMIN_USERS = DefaultDashboardEndpoints.DASHBOARD_ADMIN_USERS_PATH;

    // Driver dashboard
    public static final String DRIVER_DASHBOARD = DriverDashboardEndpoints.DASHBOARD_PATH;
    public static final String DRIVER_SCAN_PAGE = DriverDashboardEndpoints.SCAN_PAGE_PATH;
    public static final String DRIVER_CREATE_LABEL_PAGE = DriverDashboardEndpoints.CREATE_LABEL_PAGE_PATH;
    public static final String DRIVER_CREATE_LABEL = DriverDashboardEndpoints.CREATE_LABEL_PATH;
    public static final String DRIVER_TRANSITION_PACKAGE = DriverDashboardEndpoints.TRANSITION_PACKAGE_PATH;
    public static final String DRIVER_CHECK_LABEL_AVAILABILITY = DriverDashboardEndpoints.CHECK_LABEL_AVAILABILITY_PATH;

    // Merchant dashboard
    public static final String MERCHANT_DASHBOARD = MerchantDashboardEndpoints.DASHBOARD_PATH;
    public static final String MERCHANT_CREATE_SHIPMENT_PAGE = MerchantDashboardEndpoints.CREATE_SHIPMENT_PAGE_PATH;
    public static final String MERCHANT_SCAN_PAGE = MerchantDashboardEndpoints.SCAN_PAGE_PATH;
    public static final String MERCHANT_SHIPMENTS_PAGE = MerchantDashboardEndpoints.SHIPMENTS_PAGE_PATH;
    public static final String MERCHANT_GET_ALL_SHIPMENTS = MerchantDashboardEndpoints.GET_ALL_SHIPMENTS_PATH;
    public static final String MERCHANT_UPDATE_SHIPMENT = MerchantDashboardEndpoints.UPDATE_SHIPMENT_PATH;
    public static final String MERCHANT_CREATE_SHIPMENT = MerchantDashboardEndpoints.CREATE_SHIPMENT_PATH;
    public static final String MERCHANT_ROUTES_PAGE = MerchantDashboardEndpoints.ROUTES_PAGE_PATH;
    public static final String MERCHANT_CREATE_ROUTE = MerchantDashboardEndpoints.CREATE_ROUTE_PATH;
    public static final String MERCHANT_UPDATE_ROUTE = MerchantDashboardEndpoints.UPDATE_ROUTE_PATH;
    public static final String MERCHANT_DELETE_ROUTE = MerchantDashboardEndpoints.DELETE_ROUTE_PATH;

    // Superuser dashboard
    public static final String SUPERUSER_DASHBOARD = SuperUserDashboardEndpoints.DASHBOARD_PATH;
    public static final String SUPERUSER_CREATE_SHIPMENT_PAGE = SuperUserDashboardEndpoints.CREATE_SHIPMENT_PAGE_PATH;
    public static final String SUPERUSER_GET_ALL_SHIPMENTS = SuperUserDashboardEndpoints.GET_ALL_SHIPMENTS_PATH;
    public static final String SUPERUSER_FORCE_UPDATE_SHIPMENT = SuperUserDashboardEndpoints.FORCE_UPDATE_SHIPMENT_PATH;

    // Shipping labels
    public static final String SHIPPING_LABELS = ShippingLabelEndpoints.SHIPPING_LABELS_PATH;
    public static final String SHIPPING_LABEL_DETAIL_TEMPLATE = ShippingLabelEndpoints.SHIPPING_LABEL_DETAIL_PATH;
    public static final String SHIPPING_LABEL_PDF_TEMPLATE = ShippingLabelEndpoints.SHIPPING_LABEL_PDF_PATH;

    // Camera
    public static final String CAMERA_STREAM_FRAME = CameraFeedEndpoints.STREAM_FRAME_PATH;

    // Auth endpoints
    public static final String AUTH_LOGIN = UserAuthEndpoints.LOGIN_PATH;
    public static final String AUTH_LOGOUT = UserAuthEndpoints.LOGOUT_PATH;
    public static final String AUTH_CHECK_SESSION = UserAuthEndpoints.CHECK_SESSION_PATH;
    public static final String AUTH_PROMOTE_USER = UserAuthEndpoints.PROMOTE_USER_PATH;
    public static final String AUTH_DEMOTE_USER = UserAuthEndpoints.DEMOTE_USER_PATH;
    public static final String AUTH_DELETE_USER = UserAuthEndpoints.DELETE_USER_PATH;
    public static final String AUTH_CREATE_MERCHANT = UserAuthEndpoints.CREATE_MERCHANT_PATH;
    public static final String AUTH_DISABLE_USER = UserAuthEndpoints.DISABLE_USER_PATH;

    // Config
    public static final String HANDSHAKE = ClientConfigEndpoints.HANDSHAKE_PATH;

    // Registration
    public static final String REGISTER = "/api/register";

    public static String home() {
        return HOME;
    }

    public static String login() {
        return LOGIN;
    }

    public static String trackingPage() {
        return TRACKING_PAGE;
    }

    public static String trackingDetail(String trackingNumber) {
        return TRACKING_DETAIL_TEMPLATE.replace("{trackingNumber}", trackingNumber);
    }

    public static String authLogin() {
        return AUTH_LOGIN;
    }

    public static String authLogout() {
        return AUTH_LOGOUT;
    }

    public static String authCheckSession() {
        return AUTH_CHECK_SESSION;
    }

    public static String promoteUser() {
        return AUTH_PROMOTE_USER;
    }

    public static String demoteUser() {
        return AUTH_DEMOTE_USER;
    }

    public static String deleteUser() {
        return AUTH_DELETE_USER;
    }

    public static String createMerchant() {
        return AUTH_CREATE_MERCHANT;
    }

    public static String disableUser() {
        return AUTH_DISABLE_USER;
    }

    public static String handshake() {
        return HANDSHAKE;
    }

    public static String shippingLabels() {
        return SHIPPING_LABELS;
    }

    public static String shippingLabelDetail(String uuid) {
        return SHIPPING_LABEL_DETAIL_TEMPLATE.replace("{uuid}", uuid);
    }

    public static String shippingLabelPdf(String uuid) {
        return SHIPPING_LABEL_PDF_TEMPLATE.replace("{uuid}", uuid);
    }

    public static String trackingSearch() {
        return TrackingEndpoints.SEARCH_TRACKING.endpoint();
    }

    public static String trackingBatch() {
        return TrackingEndpoints.BATCH_TRACKING.endpoint();
    }

    public static String trackingDetails() {
        return TrackingEndpoints.GET_SHIPMENT_DETAILS.endpoint();
    }

    public static String scanPackage() {
        return CameraFeedEndpoints.SCAN_PACKAGE.endpoint();
    }

    public static String confirmRoute() {
        return CameraFeedEndpoints.CONFIRM_ROUTE.endpoint();
    }

    public static String streamFrame() {
        return CAMERA_STREAM_FRAME;
    }

    public static String dashboard() {
        return DASHBOARD;
    }

    public static String dashboardHomeAlias() {
        return DASHBOARD_HOME_ALIAS;
    }

    public static String dashboardLoginHome() {
        return DASHBOARD_LOGIN_HOME;
    }

    public static String dashboardLogout() {
        return DASHBOARD_LOGOUT;
    }

    public static String dashboardAccessDenied() {
        return DASHBOARD_ACCESS_DENIED;
    }

    public static String dashboardError() {
        return DASHBOARD_ERROR;
    }

    public static String dashboardStatus() {
        return DASHBOARD_STATUS;
    }

    public static String dashboardAdminUsers() {
        return DASHBOARD_ADMIN_USERS;
    }

    public static String driverDashboard() {
        return DRIVER_DASHBOARD;
    }

    public static String driverScanPage() {
        return DRIVER_SCAN_PAGE;
    }

    public static String driverCreateLabelPage() {
        return DRIVER_CREATE_LABEL_PAGE;
    }

    public static String driverCreateLabel() {
        return DRIVER_CREATE_LABEL;
    }

    public static String driverTransitionPackage() {
        return DRIVER_TRANSITION_PACKAGE;
    }

    public static String driverCheckLabelAvailability() {
        return DRIVER_CHECK_LABEL_AVAILABILITY;
    }

    public static String merchantDashboard() {
        return MERCHANT_DASHBOARD;
    }

    public static String merchantCreateShipmentPage() {
        return MERCHANT_CREATE_SHIPMENT_PAGE;
    }

    public static String merchantScanPage() {
        return MERCHANT_SCAN_PAGE;
    }

    public static String merchantShipmentsPage() {
        return MERCHANT_SHIPMENTS_PAGE;
    }

    public static String merchantRoutesPage() {
        return MERCHANT_ROUTES_PAGE;
    }

    public static String merchantGetAllShipments() {
        return MERCHANT_GET_ALL_SHIPMENTS;
    }

    public static String merchantUpdateShipment() {
        return MERCHANT_UPDATE_SHIPMENT;
    }

    public static String merchantDeleteShipment() {
        return MerchantDashboardEndpoints.DELETE_SHIPMENT.endpoint();
    }

    public static String merchantCreateShipment() {
        return MERCHANT_CREATE_SHIPMENT;
    }

    public static String merchantCreateRoute() {
        return MERCHANT_CREATE_ROUTE;
    }

    public static String merchantUpdateRoute() {
        return MERCHANT_UPDATE_ROUTE;
    }

    public static String merchantDeleteRoute() {
        return MERCHANT_DELETE_ROUTE;
    }

    public static String merchantGenerateQr() {
        return MerchantDashboardEndpoints.GENERATE_QR.endpoint();
    }

    public static String merchantAsyncBatchScan() {
        return MerchantDashboardEndpoints.ASYNC_BATCH_SCAN.endpoint();
    }

    public static String merchantGetBatchErrors() {
        return MerchantDashboardEndpoints.GET_BATCH_ERRORS.endpoint();
    }

    public static String merchantResolveBatchError() {
        return MerchantDashboardEndpoints.RESOLVE_BATCH_ERROR.endpoint();
    }

    public static String merchantCompleteBatchSession() {
        return MerchantDashboardEndpoints.COMPLETE_BATCH_SESSION.endpoint();
    }

    public static String superUserDashboard() {
        return SUPERUSER_DASHBOARD;
    }

    public static String superUserCreateShipmentPage() {
        return SUPERUSER_CREATE_SHIPMENT_PAGE;
    }

    public static String superUserGetAllShipments() {
        return SUPERUSER_GET_ALL_SHIPMENTS;
    }

    public static String superUserForceUpdateShipment() {
        return SUPERUSER_FORCE_UPDATE_SHIPMENT;
    }

    public static String superUserDestroyShipment() {
        return SuperUserDashboardEndpoints.DESTROY_SHIPMENT.endpoint();
    }

    public static String superUserGetAllUsers() {
        return SuperUserDashboardEndpoints.GET_ALL_USERS.endpoint();
    }

    public static String superUserForceUpdateUserRole() {
        return SuperUserDashboardEndpoints.FORCE_UPDATE_USER_ROLE.endpoint();
    }

    public static String superUserForceDeleteUser() {
        return SuperUserDashboardEndpoints.FORCE_DELETE_USER.endpoint();
    }

    public static String superUserGetAllStatusDefinitions() {
        return SuperUserDashboardEndpoints.GET_ALL_STATUS_DEFINITIONS.endpoint();
    }

    public static String superUserCreateOrUpdateStatus() {
        return SuperUserDashboardEndpoints.CREATE_OR_UPDATE_STATUS.endpoint();
    }

    public static String superUserDeleteStatusDefinition() {
        return SuperUserDashboardEndpoints.DELETE_STATUS_DEFINITION.endpoint();
    }
}
