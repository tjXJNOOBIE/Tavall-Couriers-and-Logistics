package org.tavall.couriers.web.view;

import org.tavall.couriers.api.web.entities.ShippingLabelMetaDataEntity;

record LabelCreationResult(ShippingLabelMetaDataEntity entity, boolean existing) {
}
