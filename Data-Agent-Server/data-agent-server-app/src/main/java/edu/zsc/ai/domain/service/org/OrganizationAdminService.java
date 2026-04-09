package edu.zsc.ai.domain.service.org;

import edu.zsc.ai.domain.model.dto.request.org.AddOrganizationMemberRequest;
import edu.zsc.ai.domain.model.dto.request.org.CreateOrganizationRequest;
import edu.zsc.ai.domain.model.dto.response.org.ManagedOrganizationResponse;
import edu.zsc.ai.domain.model.dto.response.org.MyOrganizationMembershipResponse;
import edu.zsc.ai.domain.model.dto.response.org.OrganizationMemberRowResponse;

import java.util.List;

public interface OrganizationAdminService {

    List<ManagedOrganizationResponse> listManagedOrganizations(long operatorUserId);

    List<MyOrganizationMembershipResponse> listMyMemberships(long userId);

    void leaveOrganization(long operatorUserId, long orgId);

    ManagedOrganizationResponse createOrganization(long operatorUserId, CreateOrganizationRequest request);

    List<OrganizationMemberRowResponse> listMembers(long operatorUserId, long orgId);

    void addOrUpdateMember(long operatorUserId, long orgId, AddOrganizationMemberRequest request);

    void removeCommonMember(long operatorUserId, long orgId, long memberId);
}
