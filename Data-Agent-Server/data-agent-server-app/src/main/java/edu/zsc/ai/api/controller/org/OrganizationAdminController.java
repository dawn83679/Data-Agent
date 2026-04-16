package edu.zsc.ai.api.controller.org;

import cn.dev33.satoken.stp.StpUtil;
import edu.zsc.ai.domain.model.dto.request.org.AddOrganizationMemberRequest;
import edu.zsc.ai.domain.model.dto.request.org.CreateOrganizationRequest;
import edu.zsc.ai.domain.model.dto.response.base.ApiResponse;
import edu.zsc.ai.domain.model.dto.response.org.ManagedOrganizationResponse;
import edu.zsc.ai.domain.model.dto.response.org.MyOrganizationMembershipResponse;
import edu.zsc.ai.domain.model.dto.response.org.OrganizationMemberRowResponse;
import edu.zsc.ai.domain.service.org.OrganizationAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/organizations")
@Validated
@RequiredArgsConstructor
public class OrganizationAdminController {

    private final OrganizationAdminService organizationAdminService;

    /**
     * Organizations where the current user is an active ADMIN (for management UI).
     */
    @GetMapping("/managed")
    public ApiResponse<List<ManagedOrganizationResponse>> listManaged() {
        long userId = StpUtil.getLoginIdAsLong();
        return ApiResponse.success(organizationAdminService.listManagedOrganizations(userId));
    }

    @GetMapping("/memberships")
    public ApiResponse<List<MyOrganizationMembershipResponse>> listMyMemberships() {
        long userId = StpUtil.getLoginIdAsLong();
        return ApiResponse.success(organizationAdminService.listMyMemberships(userId));
    }

    @DeleteMapping("/{orgId}/members/me")
    public ApiResponse<Void> leaveOrganization(@PathVariable long orgId) {
        long userId = StpUtil.getLoginIdAsLong();
        organizationAdminService.leaveOrganization(userId, orgId);
        return ApiResponse.success();
    }

    @PostMapping
    public ApiResponse<ManagedOrganizationResponse> create(@Valid @RequestBody CreateOrganizationRequest request) {
        long userId = StpUtil.getLoginIdAsLong();
        return ApiResponse.success(organizationAdminService.createOrganization(userId, request));
    }

    @GetMapping("/{orgId}/members")
    public ApiResponse<List<OrganizationMemberRowResponse>> listMembers(@PathVariable long orgId) {
        long userId = StpUtil.getLoginIdAsLong();
        return ApiResponse.success(organizationAdminService.listMembers(userId, orgId));
    }

    @PostMapping("/{orgId}/members")
    public ApiResponse<Void> addMember(@PathVariable long orgId, @Valid @RequestBody AddOrganizationMemberRequest request) {
        long userId = StpUtil.getLoginIdAsLong();
        organizationAdminService.addOrUpdateMember(userId, orgId, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{orgId}/members/{memberId}")
    public ApiResponse<Void> removeMember(@PathVariable long orgId, @PathVariable long memberId) {
        long userId = StpUtil.getLoginIdAsLong();
        organizationAdminService.removeCommonMember(userId, orgId, memberId);
        return ApiResponse.success();
    }
}
