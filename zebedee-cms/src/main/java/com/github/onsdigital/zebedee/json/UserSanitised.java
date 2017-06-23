package com.github.onsdigital.zebedee.json;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Represents a reduced view of user account, suitable for sending to clients via the API.
 * NB this record intentionally does not contain any authentication, encryption or permission-related information.
 * This is purely acconut information.
 */
public class UserSanitised {

    public String name;
    public String email;
    public String ownerEmail;

    /**
     * This field is {@link Boolean} rather than <code>boolean</code> so that it can be <code>null</code> in an update message.
     * This ensures the value won't change unless explicitly specified.
     */
    public Boolean inactive;

    public Boolean temporaryPassword;
    public String lastAdmin;

    public AdminOptions adminOptions;

    @Override
    public String toString() {
        return name + ", " + email + (BooleanUtils.isTrue(inactive) ? " (inactive)" : "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        UserSanitised that = (UserSanitised) o;

        return new EqualsBuilder()
                .append(name, that.name)
                .append(email, that.email)
                .append(ownerEmail, that.ownerEmail)
                .append(inactive, that.inactive)
                .append(lastAdmin, that.lastAdmin)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(name)
                .append(email)
                .append(ownerEmail)
                .append(inactive)
                .append(lastAdmin)
                .toHashCode();
    }
}
