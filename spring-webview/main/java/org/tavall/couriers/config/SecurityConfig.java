package org.tavall.couriers.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.tavall.couriers.api.web.permission.PermissionMapper;
import org.tavall.couriers.api.web.endpoints.dashboard.DefaultDashboardEndpoints;
import org.tavall.couriers.api.web.service.user.UserAccountService;
import org.tavall.couriers.api.web.user.permission.Role;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private UserAccountService userService;
    private PermissionMapper permissionMapper;



    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // You have a login page at /dashboard/login, so let people reach it.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",                      // public home, splash style
                                "/dashboard/login",       // GET login page
                                "/dashboard",             // dash home, same as /dashboard/login
                                "/tracking",              // public tracking entry
                                "/tracking/**",           // public tracking detail
                                "/css/**",
                                "/js/**",
                                "/images/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                // Your custom thymeleaf login page + processing endpoint
                .formLogin(form -> form
                        .loginPage("/dashboard/login")          // GET shows your template
                        .loginProcessingUrl("/dashboard/login") // POST submits here (your form already does this)
                        .defaultSuccessUrl("/dashboard", true)  // where to go after login
                        .failureUrl("/dashboard/login?error=true")
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl(DefaultDashboardEndpoints.DASHBOARD_LOGOUT_PATH)
                        .logoutSuccessUrl("/dashboard/login?logout=true")
                )

                // Make unauthenticated visitors exist but have NO roles
                .anonymous(anon -> anon
                        .principal("guest")
                )

                // keep defaults
                .csrf(Customizer.withDefaults());

        return http.build();
    }
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        UserDetails driver = User.withUsername("driver")
                .password(encoder.encode("driver"))
                .authorities(Role.DRIVER.grantedAuthorities())
                .build();

        UserDetails merchant = User.withUsername("merchant")
                .password(encoder.encode("merchant"))
                .authorities(Role.MERCHANT.grantedAuthorities())
                .build();

        UserDetails superuser = User.withUsername("superuser")
                .password(encoder.encode("superuser"))
                .authorities(Role.SUPERUSER.grantedAuthorities())
                .build();

        UserDetails user = User.withUsername("user")
                .password(encoder.encode("user"))
                .authorities(Role.USER.grantedAuthorities())
                .build();
        return new InMemoryUserDetailsManager(driver, merchant, superuser, user);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
