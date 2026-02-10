package org.tavall.couriers.api.web.utils.permission;


import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.tavall.couriers.api.web.user.UserAccountEntity;
import org.tavall.couriers.api.web.user.permission.Role;
import org.tavall.couriers.api.web.user.permission.UserPermissions;

import java.security.Permission;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
@Component
public class PermissionMapper {



    public Collection<? extends GrantedAuthority> map(UserAccountEntity account) {
        List<GrantedAuthority> out = new ArrayList<>();
        for (Role role : account.getRoles()) {
            out.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
        }
        for (UserPermissions perm : account.permissions()) {
            out.add(new SimpleGrantedAuthority("PERM_" + perm.name()));
        }

        return Collections.unmodifiableList(out);
    }
}