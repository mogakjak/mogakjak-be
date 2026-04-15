package com.mogakjak.mogakjak.domain.user.repository.projection;

import java.util.UUID;

public interface SharedGroupNameProjection {

    UUID getMateId();

    String getGroupName();
}
