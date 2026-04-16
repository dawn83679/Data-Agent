package edu.zsc.ai.domain.service.org.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.zsc.ai.common.constant.ResponseCode;
import edu.zsc.ai.common.constant.ResponseMessageKey;
import edu.zsc.ai.common.enums.org.OrganizationRoleEnum;
import edu.zsc.ai.domain.exception.BusinessException;
import edu.zsc.ai.domain.mapper.sys.SysOrganizationMapper;
import edu.zsc.ai.domain.mapper.sys.SysOrganizationMemberMapper;
import edu.zsc.ai.domain.mapper.sys.SysOrganizationMemberRoleMapper;
import edu.zsc.ai.domain.model.entity.sys.SysOrganization;
import edu.zsc.ai.domain.model.entity.sys.SysOrganizationMember;
import edu.zsc.ai.domain.model.entity.sys.SysOrganizationMemberRole;
import edu.zsc.ai.domain.service.org.OrgAccessService;
import edu.zsc.ai.domain.service.org.OrgMemberContext;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Organization access checks backed by sys_organization* tables.
 */
@Service
public class OrgAccessServiceImpl implements OrgAccessService {

    private static final int ORG_ENABLED = 1;
    private static final int MEMBER_ACTIVE = 1;

    @Resource
    private SysOrganizationMapper sysOrganizationMapper;
    @Resource
    private SysOrganizationMemberMapper sysOrganizationMemberMapper;
    @Resource
    private SysOrganizationMemberRoleMapper sysOrganizationMemberRoleMapper;

    @Override
    public OrgMemberContext loadActiveMembership(long userId, long orgId) {
        SysOrganization org = sysOrganizationMapper.selectById(orgId);
        if (org == null || org.getStatus() == null || org.getStatus() != ORG_ENABLED) {
            throw BusinessException.of(ResponseCode.PARAM_ERROR, ResponseMessageKey.WORKSPACE_ORG_INVALID);
        }

        SysOrganizationMember member = sysOrganizationMemberMapper.selectOne(new LambdaQueryWrapper<SysOrganizationMember>()
                .eq(SysOrganizationMember::getOrgId, orgId)
                .eq(SysOrganizationMember::getUserId, userId)
                .eq(SysOrganizationMember::getStatus, MEMBER_ACTIVE));

        if (member == null) {
            throw BusinessException.of(ResponseCode.FORBIDDEN, ResponseMessageKey.WORKSPACE_ORG_NOT_MEMBER);
        }

        SysOrganizationMemberRole roleRow = sysOrganizationMemberRoleMapper.selectOne(
                new LambdaQueryWrapper<SysOrganizationMemberRole>()
                        .eq(SysOrganizationMemberRole::getOrganizationMemberId, member.getId())
                        .eq(SysOrganizationMemberRole::getActive, true));

        if (roleRow == null || roleRow.getRoleCode() == null || roleRow.getRoleCode().isBlank()) {
            throw BusinessException.of(ResponseCode.FORBIDDEN, ResponseMessageKey.WORKSPACE_ORG_NO_ROLE);
        }

        OrganizationRoleEnum role = parseRoleCode(roleRow.getRoleCode());
        return new OrgMemberContext(orgId, member.getId(), role);
    }

    @Override
    public List<Long> listActiveOrgAdminUserIds(long orgId) {
        SysOrganization org = sysOrganizationMapper.selectById(orgId);
        if (org == null || org.getStatus() == null || org.getStatus() != ORG_ENABLED) {
            return List.of();
        }
        List<SysOrganizationMember> members = sysOrganizationMemberMapper.selectList(
                new LambdaQueryWrapper<SysOrganizationMember>()
                        .eq(SysOrganizationMember::getOrgId, orgId)
                        .eq(SysOrganizationMember::getStatus, MEMBER_ACTIVE));
        List<Long> adminUserIds = new ArrayList<>();
        for (SysOrganizationMember m : members) {
            SysOrganizationMemberRole roleRow = sysOrganizationMemberRoleMapper.selectOne(
                    new LambdaQueryWrapper<SysOrganizationMemberRole>()
                            .eq(SysOrganizationMemberRole::getOrganizationMemberId, m.getId())
                            .eq(SysOrganizationMemberRole::getActive, true));
            if (roleRow != null
                    && OrganizationRoleEnum.ADMIN.name().equalsIgnoreCase(roleRow.getRoleCode())) {
                if (m.getUserId() != null && !adminUserIds.contains(m.getUserId())) {
                    adminUserIds.add(m.getUserId());
                }
            }
        }
        return adminUserIds;
    }

    private static OrganizationRoleEnum parseRoleCode(String roleCode) {
        try {
            return OrganizationRoleEnum.valueOf(roleCode.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw BusinessException.of(ResponseCode.PARAM_ERROR, ResponseMessageKey.WORKSPACE_ORG_ROLE_INVALID);
        }
    }
}
