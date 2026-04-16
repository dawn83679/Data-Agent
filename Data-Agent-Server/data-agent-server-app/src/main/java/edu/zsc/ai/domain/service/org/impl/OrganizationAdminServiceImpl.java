package edu.zsc.ai.domain.service.org.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import edu.zsc.ai.common.constant.ResponseCode;
import edu.zsc.ai.common.constant.ResponseMessageKey;
import edu.zsc.ai.common.enums.org.OrganizationRoleEnum;
import edu.zsc.ai.domain.exception.BusinessException;
import edu.zsc.ai.domain.mapper.sys.SysOrganizationMapper;
import edu.zsc.ai.domain.mapper.sys.SysOrganizationMemberMapper;
import edu.zsc.ai.domain.mapper.sys.SysOrganizationMemberRoleMapper;
import edu.zsc.ai.domain.mapper.sys.SysUsersMapper;
import edu.zsc.ai.domain.model.dto.request.org.AddOrganizationMemberRequest;
import edu.zsc.ai.domain.model.dto.request.org.CreateOrganizationRequest;
import edu.zsc.ai.domain.model.dto.response.org.ManagedOrganizationResponse;
import edu.zsc.ai.domain.model.dto.response.org.MyOrganizationMembershipResponse;
import edu.zsc.ai.domain.model.dto.response.org.OrganizationMemberRowResponse;
import edu.zsc.ai.domain.model.entity.sys.SysOrganization;
import edu.zsc.ai.domain.model.entity.sys.SysOrganizationMember;
import edu.zsc.ai.domain.model.entity.sys.SysOrganizationMemberRole;
import edu.zsc.ai.domain.model.entity.sys.SysUsers;
import edu.zsc.ai.domain.service.org.OrganizationAdminService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class OrganizationAdminServiceImpl implements OrganizationAdminService {

    private static final int ORG_ENABLED = 1;
    private static final int MEMBER_ACTIVE = 1;

    @Resource
    private SysOrganizationMapper sysOrganizationMapper;
    @Resource
    private SysOrganizationMemberMapper sysOrganizationMemberMapper;
    @Resource
    private SysOrganizationMemberRoleMapper sysOrganizationMemberRoleMapper;
    @Resource
    private SysUsersMapper sysUsersMapper;

    @Override
    public List<ManagedOrganizationResponse> listManagedOrganizations(long operatorUserId) {
        List<SysOrganizationMember> myMembers = sysOrganizationMemberMapper.selectList(
                new LambdaQueryWrapper<SysOrganizationMember>()
                        .eq(SysOrganizationMember::getUserId, operatorUserId)
                        .eq(SysOrganizationMember::getStatus, MEMBER_ACTIVE));
        List<ManagedOrganizationResponse> out = new ArrayList<>();
        for (SysOrganizationMember m : myMembers) {
            if (!isActiveAdminRole(m.getId())) {
                continue;
            }
            SysOrganization org = sysOrganizationMapper.selectById(m.getOrgId());
            if (org == null || org.getStatus() == null || org.getStatus() != ORG_ENABLED) {
                continue;
            }
            out.add(ManagedOrganizationResponse.builder()
                    .id(org.getId())
                    .orgCode(org.getOrgCode())
                    .orgName(org.getOrgName())
                    .build());
        }
        return out;
    }

    @Override
    public List<MyOrganizationMembershipResponse> listMyMemberships(long userId) {
        List<SysOrganizationMember> myMembers = sysOrganizationMemberMapper.selectList(
                new LambdaQueryWrapper<SysOrganizationMember>()
                        .eq(SysOrganizationMember::getUserId, userId)
                        .eq(SysOrganizationMember::getStatus, MEMBER_ACTIVE));
        List<MyOrganizationMembershipResponse> out = new ArrayList<>();
        for (SysOrganizationMember m : myMembers) {
            SysOrganization org = sysOrganizationMapper.selectById(m.getOrgId());
            if (org == null || org.getStatus() == null || org.getStatus() != ORG_ENABLED) {
                continue;
            }
            SysOrganizationMemberRole roleRow = sysOrganizationMemberRoleMapper.selectOne(
                    new LambdaQueryWrapper<SysOrganizationMemberRole>()
                            .eq(SysOrganizationMemberRole::getOrganizationMemberId, m.getId())
                            .eq(SysOrganizationMemberRole::getActive, true));
            String roleCode = roleRow != null && roleRow.getRoleCode() != null
                    ? roleRow.getRoleCode().trim().toUpperCase(Locale.ROOT)
                    : "";
            if (roleCode.isEmpty()) {
                continue;
            }
            out.add(MyOrganizationMembershipResponse.builder()
                    .id(org.getId())
                    .orgCode(org.getOrgCode())
                    .orgName(org.getOrgName())
                    .roleCode(roleCode)
                    .build());
        }
        return out;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveOrganization(long operatorUserId, long orgId) {
        SysOrganizationMember member = sysOrganizationMemberMapper.selectOne(
                new LambdaQueryWrapper<SysOrganizationMember>()
                        .eq(SysOrganizationMember::getOrgId, orgId)
                        .eq(SysOrganizationMember::getUserId, operatorUserId)
                        .eq(SysOrganizationMember::getStatus, MEMBER_ACTIVE));
        if (member == null) {
            throw BusinessException.of(ResponseCode.PARAM_ERROR, ResponseMessageKey.ORG_MEMBER_NOT_FOUND);
        }
        String roleCode = activeRoleCode(member.getId());
        if (OrganizationRoleEnum.ADMIN.name().equals(roleCode) && countActiveAdmins(orgId) <= 1) {
            throw BusinessException.of(ResponseCode.PARAM_ERROR, ResponseMessageKey.ORG_LAST_ADMIN_FORBIDDEN);
        }
        deactivateMemberRoles(member.getId());
        member.setStatus(0);
        member.setUpdatedAt(LocalDateTime.now());
        sysOrganizationMemberMapper.updateById(member);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ManagedOrganizationResponse createOrganization(long operatorUserId, CreateOrganizationRequest request) {
        String code = request.getOrgCode().trim();
        Long exists = sysOrganizationMapper.selectCount(
                new LambdaQueryWrapper<SysOrganization>().eq(SysOrganization::getOrgCode, code));
        if (exists != null && exists > 0) {
            throw BusinessException.of(ResponseCode.PARAM_ERROR, ResponseMessageKey.ORG_CODE_EXISTS);
        }

        SysOrganization org = new SysOrganization();
        org.setOrgCode(code);
        org.setOrgName(request.getOrgName().trim());
        org.setStatus(ORG_ENABLED);
        org.setCreatedBy(operatorUserId);
        org.setCreatedAt(LocalDateTime.now());
        org.setUpdatedAt(LocalDateTime.now());
        sysOrganizationMapper.insert(org);

        SysOrganizationMember member = new SysOrganizationMember();
        member.setOrgId(org.getId());
        member.setUserId(operatorUserId);
        member.setStatus(MEMBER_ACTIVE);
        member.setJoinedAt(LocalDateTime.now());
        member.setCreatedAt(LocalDateTime.now());
        member.setUpdatedAt(LocalDateTime.now());
        sysOrganizationMemberMapper.insert(member);

        SysOrganizationMemberRole role = new SysOrganizationMemberRole();
        role.setOrganizationMemberId(member.getId());
        role.setRoleCode(OrganizationRoleEnum.ADMIN.name());
        role.setActive(true);
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        sysOrganizationMemberRoleMapper.insert(role);

        return ManagedOrganizationResponse.builder()
                .id(org.getId())
                .orgCode(org.getOrgCode())
                .orgName(org.getOrgName())
                .build();
    }

    @Override
    public List<OrganizationMemberRowResponse> listMembers(long operatorUserId, long orgId) {
        assertOrgAdmin(operatorUserId, orgId);
        List<SysOrganizationMember> members = sysOrganizationMemberMapper.selectList(
                new LambdaQueryWrapper<SysOrganizationMember>()
                        .eq(SysOrganizationMember::getOrgId, orgId)
                        .eq(SysOrganizationMember::getStatus, MEMBER_ACTIVE)
                        .orderByAsc(SysOrganizationMember::getId));
        List<OrganizationMemberRowResponse> rows = new ArrayList<>();
        for (SysOrganizationMember m : members) {
            SysUsers user = sysUsersMapper.selectById(m.getUserId());
            SysOrganizationMemberRole roleRow = sysOrganizationMemberRoleMapper.selectOne(
                    new LambdaQueryWrapper<SysOrganizationMemberRole>()
                            .eq(SysOrganizationMemberRole::getOrganizationMemberId, m.getId())
                            .eq(SysOrganizationMemberRole::getActive, true));
            String roleCode = roleRow != null && roleRow.getRoleCode() != null
                    ? roleRow.getRoleCode().trim().toUpperCase(Locale.ROOT)
                    : "";
            rows.add(OrganizationMemberRowResponse.builder()
                    .memberId(m.getId())
                    .userId(m.getUserId())
                    .username(user != null ? user.getUsername() : "")
                    .email(user != null ? user.getEmail() : "")
                    .roleCode(roleCode)
                    .build());
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addOrUpdateMember(long operatorUserId, long orgId, AddOrganizationMemberRequest request) {
        assertOrgAdmin(operatorUserId, orgId);
        OrganizationRoleEnum role = parseRoleForWrite(request.getRoleCode());

        SysUsers target = sysUsersMapper.selectOne(new LambdaQueryWrapper<SysUsers>()
                .eq(SysUsers::getEmail, request.getEmail().trim()));
        if (target == null) {
            throw BusinessException.of(ResponseCode.PARAM_ERROR, ResponseMessageKey.USER_NOT_FOUND_MESSAGE);
        }

        SysOrganizationMember member = sysOrganizationMemberMapper.selectOne(
                new LambdaQueryWrapper<SysOrganizationMember>()
                        .eq(SysOrganizationMember::getOrgId, orgId)
                        .eq(SysOrganizationMember::getUserId, target.getId()));
        if (member == null) {
            member = new SysOrganizationMember();
            member.setOrgId(orgId);
            member.setUserId(target.getId());
            member.setStatus(MEMBER_ACTIVE);
            member.setJoinedAt(LocalDateTime.now());
            member.setCreatedAt(LocalDateTime.now());
            member.setUpdatedAt(LocalDateTime.now());
            sysOrganizationMemberMapper.insert(member);
        } else {
            if (!Objects.equals(member.getStatus(), MEMBER_ACTIVE)) {
                member.setStatus(MEMBER_ACTIVE);
                member.setUpdatedAt(LocalDateTime.now());
                sysOrganizationMemberMapper.updateById(member);
            }
        }
        deactivateMemberRoles(member.getId());
        insertActiveRole(member.getId(), role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeCommonMember(long operatorUserId, long orgId, long memberId) {
        assertOrgAdmin(operatorUserId, orgId);
        SysOrganizationMember member = sysOrganizationMemberMapper.selectById(memberId);
        if (member == null || !Objects.equals(member.getOrgId(), orgId)
                || !Objects.equals(member.getStatus(), MEMBER_ACTIVE)) {
            throw BusinessException.of(ResponseCode.PARAM_ERROR, ResponseMessageKey.ORG_MEMBER_NOT_FOUND);
        }
        SysOrganizationMemberRole roleRow = sysOrganizationMemberRoleMapper.selectOne(
                new LambdaQueryWrapper<SysOrganizationMemberRole>()
                        .eq(SysOrganizationMemberRole::getOrganizationMemberId, memberId)
                        .eq(SysOrganizationMemberRole::getActive, true));
        String roleCode = roleRow != null && roleRow.getRoleCode() != null
                ? roleRow.getRoleCode().trim().toUpperCase(Locale.ROOT) : "";
        if (OrganizationRoleEnum.ADMIN.name().equals(roleCode)) {
            throw BusinessException.of(ResponseCode.FORBIDDEN, ResponseMessageKey.ORG_REMOVE_ADMIN_FORBIDDEN);
        }
        if (roleRow != null) {
            roleRow.setActive(false);
            roleRow.setUpdatedAt(LocalDateTime.now());
            sysOrganizationMemberRoleMapper.updateById(roleRow);
        }
        member.setStatus(0);
        member.setUpdatedAt(LocalDateTime.now());
        sysOrganizationMemberMapper.updateById(member);
    }

    private void assertOrgAdmin(long operatorUserId, long orgId) {
        SysOrganization org = sysOrganizationMapper.selectById(orgId);
        if (org == null || org.getStatus() == null || org.getStatus() != ORG_ENABLED) {
            throw BusinessException.of(ResponseCode.PARAM_ERROR, ResponseMessageKey.WORKSPACE_ORG_INVALID);
        }
        SysOrganizationMember m = sysOrganizationMemberMapper.selectOne(
                new LambdaQueryWrapper<SysOrganizationMember>()
                        .eq(SysOrganizationMember::getOrgId, orgId)
                        .eq(SysOrganizationMember::getUserId, operatorUserId)
                        .eq(SysOrganizationMember::getStatus, MEMBER_ACTIVE));
        if (m == null || !isActiveAdminRole(m.getId())) {
            throw BusinessException.of(ResponseCode.FORBIDDEN, ResponseMessageKey.ORG_ADMIN_REQUIRED);
        }
    }

    private boolean isActiveAdminRole(long organizationMemberId) {
        return OrganizationRoleEnum.ADMIN.name().equals(activeRoleCode(organizationMemberId));
    }

    private String activeRoleCode(long organizationMemberId) {
        SysOrganizationMemberRole role = sysOrganizationMemberRoleMapper.selectOne(
                new LambdaQueryWrapper<SysOrganizationMemberRole>()
                        .eq(SysOrganizationMemberRole::getOrganizationMemberId, organizationMemberId)
                        .eq(SysOrganizationMemberRole::getActive, true));
        return role != null && role.getRoleCode() != null
                ? role.getRoleCode().trim().toUpperCase(Locale.ROOT)
                : "";
    }

    private long countActiveAdmins(long orgId) {
        List<SysOrganizationMember> members = sysOrganizationMemberMapper.selectList(
                new LambdaQueryWrapper<SysOrganizationMember>()
                        .eq(SysOrganizationMember::getOrgId, orgId)
                        .eq(SysOrganizationMember::getStatus, MEMBER_ACTIVE));
        long n = 0;
        for (SysOrganizationMember m : members) {
            if (OrganizationRoleEnum.ADMIN.name().equals(activeRoleCode(m.getId()))) {
                n++;
            }
        }
        return n;
    }

    private static OrganizationRoleEnum parseRoleForWrite(String raw) {
        if (raw == null || raw.isBlank()) {
            throw BusinessException.of(ResponseCode.PARAM_ERROR, ResponseMessageKey.WORKSPACE_ORG_ROLE_INVALID);
        }
        try {
            return OrganizationRoleEnum.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw BusinessException.of(ResponseCode.PARAM_ERROR, ResponseMessageKey.WORKSPACE_ORG_ROLE_INVALID);
        }
    }

    private void deactivateMemberRoles(long organizationMemberId) {
        sysOrganizationMemberRoleMapper.update(null, new LambdaUpdateWrapper<SysOrganizationMemberRole>()
                .eq(SysOrganizationMemberRole::getOrganizationMemberId, organizationMemberId)
                .set(SysOrganizationMemberRole::getActive, false)
                .set(SysOrganizationMemberRole::getUpdatedAt, LocalDateTime.now()));
    }

    private void insertActiveRole(long organizationMemberId, OrganizationRoleEnum role) {
        SysOrganizationMemberRole row = new SysOrganizationMemberRole();
        row.setOrganizationMemberId(organizationMemberId);
        row.setRoleCode(role.name());
        row.setActive(true);
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        sysOrganizationMemberRoleMapper.insert(row);
    }
}
