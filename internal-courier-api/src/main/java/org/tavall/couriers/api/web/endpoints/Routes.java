package org.tavall.couriers.api.web.endpoints;

import org.springframework.stereotype.Component;
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
// This class is used to define thymeleaf end points in HTML
@Component("routes")
public class Routes {


    public String home() {
        return PageViewEndpoints.HOME.endpoint();
    }

    public String login() {
        return PageViewEndpoints.LOGIN.endpoint();
    }

    public String authLogin() {
        return UserAuthEndpoints.LOGIN.endpoint();
    }

    public String authLogout() {
        return UserAuthEndpoints.LOGOUT.endpoint();
    }

    public String authCheckSession() {
        return UserAuthEndpoints.CHECK_SESSION.endpoint();
    }

    public String promoteUser() {
        return UserAuthEndpoints.PROMOTE_USER.endpoint();
    }

    public String demoteUser() {
        return UserAuthEndpoints.DEMOTE_USER.endpoint();
    }

    public String deleteUser() {
        return UserAuthEndpoints.DELETE_USER.endpoint();
    }

    public String createMerchant() {
        return UserAuthEndpoints.CREATE_MERCHANT.endpoint();
    }

    public String disableUser() {
        return UserAuthEndpoints.DISABLE_USER.endpoint();
    }

    public String handshake() {
        return ClientConfigEndpoints.HANDSHAKE.endpoint();
    }

    public String shippingLabels() {
        return ShippingLabelEndpoints.SHIPPING_LABELS.endpoint();
    }

    public String shippingLabelDetail(String uuid) {
        return ShippingLabelEndpoints.SHIPPING_LABEL_DETAIL.endpoint().replace("{uuid}", uuid);
    }

    public String trackingSearch() {
        return TrackingEndpoints.SEARCH_TRACKING.endpoint();
    }

    public String trackingBatch() {
        return TrackingEndpoints.BATCH_TRACKING.endpoint();
    }

    public String trackingDetails() {
        return TrackingEndpoints.GET_SHIPMENT_DETAILS.endpoint();
    }

    public String scanPackage() {
        return CameraFeedEndpoints.SCAN_PACKAGE.endpoint();
    }

    public String confirmRoute() {
        return CameraFeedEndpoints.CONFIRM_ROUTE.endpoint();
    }

    public String streamFrame() {
        return CameraFeedEndpoints.STREAM_FRAME.endpoint();
    }

    public String dashboard() {
        return DefaultDashboardEndpoints.DASHBOARD_HOME.endpoint();
    }

    public String dashboardHomeAlias() {
        return DefaultDashboardEndpoints.DASHBOARD_HOME_ALIAS.endpoint();
    }

    public String dashboardLoginHome() {
        return DefaultDashboardEndpoints.DASHBOARD_LOGIN_HOME.endpoint();
    }

    public String dashboardLogout() {
        return DefaultDashboardEndpoints.DASHBOARD_LOGOUT.endpoint();
    }

    public String dashboardAccessDenied() {
        return DefaultDashboardEndpoints.DASHBOARD_ACCESS_DENIED.endpoint();
    }

    public String dashboardError() {
        return DefaultDashboardEndpoints.DASHBOARD_ERROR.endpoint();
    }

    public String dashboardStatus() {
        return DefaultDashboardEndpoints.DASHBOARD_STATUS.endpoint();
    }

    public String driverDashboard() {
        return DriverDashboardEndpoints.DASHBOARD_PATH;
    }

    public String driverCreateLabel() {
        return DriverDashboardEndpoints.CREATE_LABEL.endpoint();
    }

    public String driverTransitionPackage() {
        return DriverDashboardEndpoints.TRANSITION_PACKAGE.endpoint();
    }

    public String driverCheckLabelAvailability() {
        return DriverDashboardEndpoints.CHECK_LABEL_AVAILABILITY.endpoint();
    }

    public String merchantDashboard() {
        return MerchantDashboardEndpoints.DASHBOARD_PATH;
    }

    public String merchantCreateShipmentPage() {
        return MerchantDashboardEndpoints.CREATE_SHIPMENT_PAGE_PATH;
    }

    public String merchantGetAllShipments() {
        return MerchantDashboardEndpoints.GET_ALL_SHIPMENTS.endpoint();
    }

    public String merchantUpdateShipment() {
        return MerchantDashboardEndpoints.UPDATE_SHIPMENT.endpoint();
    }

    public String merchantDeleteShipment() {
        return MerchantDashboardEndpoints.DELETE_SHIPMENT.endpoint();
    }

    public String merchantCreateShipment() {
        return MerchantDashboardEndpoints.CREATE_SHIPMENT.endpoint();
    }

    public String merchantGenerateQr() {
        return MerchantDashboardEndpoints.GENERATE_QR.endpoint();
    }

    public String merchantAsyncBatchScan() {
        return MerchantDashboardEndpoints.ASYNC_BATCH_SCAN.endpoint();
    }

    public String merchantGetBatchErrors() {
        return MerchantDashboardEndpoints.GET_BATCH_ERRORS.endpoint();
    }

    public String merchantResolveBatchError() {
        return MerchantDashboardEndpoints.RESOLVE_BATCH_ERROR.endpoint();
    }

    public String merchantCompleteBatchSession() {
        return MerchantDashboardEndpoints.COMPLETE_BATCH_SESSION.endpoint();
    }

    public String superUserDashboard() {
        return SuperUserDashboardEndpoints.DASHBOARD_PATH;
    }

    public String superUserCreateShipmentPage() {
        return SuperUserDashboardEndpoints.CREATE_SHIPMENT_PAGE_PATH;
    }

    public String superUserGetAllShipments() {
        return SuperUserDashboardEndpoints.GET_ALL_SHIPMENTS.endpoint();
    }

    public String superUserForceUpdateShipment() {
        return SuperUserDashboardEndpoints.FORCE_UPDATE_SHIPMENT.endpoint();
    }

    public String superUserDestroyShipment() {
        return SuperUserDashboardEndpoints.DESTROY_SHIPMENT.endpoint();
    }

    public String superUserGetAllUsers() {
        return SuperUserDashboardEndpoints.GET_ALL_USERS.endpoint();
    }

    public String superUserForceUpdateUserRole() {
        return SuperUserDashboardEndpoints.FORCE_UPDATE_USER_ROLE.endpoint();
    }

    public String superUserForceDeleteUser() {
        return SuperUserDashboardEndpoints.FORCE_DELETE_USER.endpoint();
    }

    public String superUserGetAllStatusDefinitions() {
        return SuperUserDashboardEndpoints.GET_ALL_STATUS_DEFINITIONS.endpoint();
    }

    public String superUserCreateOrUpdateStatus() {
        return SuperUserDashboardEndpoints.CREATE_OR_UPDATE_STATUS.endpoint();
    }

    public String superUserDeleteStatusDefinition() {
        return SuperUserDashboardEndpoints.DELETE_STATUS_DEFINITION.endpoint();
    }
}
