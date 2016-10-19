/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.db.permission;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.GroupDto;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.security.DefaultGroups.ANYONE;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.permission.GlobalPermissions.PROVISIONING;
import static org.sonar.core.permission.GlobalPermissions.SCAN_EXECUTION;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.db.user.GroupTesting.newGroupDto;

public class GroupPermissionDaoTest {
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbSession dbSession = db.getSession();
  private GroupPermissionDao underTest = new GroupPermissionDao();

  @Test
  public void group_count_by_permission_and_component_id() {
    GroupDto group1 = db.users().insertGroup(newGroupDto());
    GroupDto group2 = db.users().insertGroup(newGroupDto());
    GroupDto group3 = db.users().insertGroup(newGroupDto());
    ComponentDto project1 = db.components().insertProject();
    ComponentDto project2 = db.components().insertProject();
    ComponentDto project3 = db.components().insertProject();

    db.users().insertProjectPermissionOnGroup(group1, ISSUE_ADMIN, project1);
    db.users().insertProjectPermissionOnGroup(group1, ADMIN, project2);
    db.users().insertProjectPermissionOnGroup(group2, ADMIN, project2);
    db.users().insertProjectPermissionOnGroup(group3, ADMIN, project2);
    // anyone group
    db.users().insertProjectPermissionOnAnyone(ADMIN, project2);
    db.users().insertProjectPermissionOnGroup(group1, USER, project2);
    db.users().insertProjectPermissionOnGroup(group1, USER, project3);

    final List<CountPerProjectPermission> result = new ArrayList<>();
    underTest.groupsCountByComponentIdAndPermission(dbSession, asList(project2.getId(), project3.getId(), 789L),
      context -> result.add((CountPerProjectPermission) context.getResultObject()));

    assertThat(result).hasSize(3);
    assertThat(result).extracting("permission").containsOnly(ADMIN, USER);
    assertThat(result).extracting("componentId").containsOnly(project2.getId(), project3.getId());
    assertThat(result).extracting("count").containsOnly(4, 1);
  }

  @Test
  public void select_groups_by_query() {
    GroupDto group1 = db.users().insertGroup(newGroupDto());
    GroupDto group2 = db.users().insertGroup(newGroupDto());
    GroupDto group3 = db.users().insertGroup(newGroupDto());
    db.users().insertPermissionOnAnyone(SCAN_EXECUTION);

    List<String> groupNames = underTest.selectGroupNamesByPermissionQuery(dbSession, PermissionQuery.builder().build());
    assertThat(groupNames).containsOnly("Anyone", group1.getName(), group2.getName(), group3.getName());
  }

  @Test
  public void select_groups_by_query_is_ordered_by_group_names() {
    db.users().insertGroup(newGroupDto().setName("Group-2"));
    db.users().insertGroup(newGroupDto().setName("Group-3"));
    db.users().insertGroup(newGroupDto().setName("Group-1"));
    db.users().insertPermissionOnAnyone(SCAN_EXECUTION);

    assertThat(underTest.selectGroupNamesByPermissionQuery(dbSession,
      PermissionQuery.builder().build())).containsExactly("Anyone", "Group-1", "Group-2", "Group-3");
  }

  @Test
  public void count_groups_by_query() {
    GroupDto group1 = db.users().insertGroup(newGroupDto().setName("Group-1"));
    GroupDto group2 = db.users().insertGroup(newGroupDto().setName("Group-2"));
    GroupDto group3 = db.users().insertGroup(newGroupDto().setName("Group-3"));
    db.users().insertPermissionOnAnyone(SCAN_EXECUTION);
    db.users().insertPermissionOnGroup(group1, PROVISIONING);

    assertThat(underTest.countGroupsByPermissionQuery(dbSession,
      PermissionQuery.builder().build())).isEqualTo(4);
    assertThat(underTest.countGroupsByPermissionQuery(dbSession,
      PermissionQuery.builder().setPermission(PROVISIONING).build())).isEqualTo(1);
    assertThat(underTest.countGroupsByPermissionQuery(dbSession,
      PermissionQuery.builder().withAtLeastOnePermission().build())).isEqualTo(2);
    assertThat(underTest.countGroupsByPermissionQuery(dbSession,
      PermissionQuery.builder().setSearchQuery("Group-").build())).isEqualTo(3);
    assertThat(underTest.countGroupsByPermissionQuery(dbSession,
      PermissionQuery.builder().setSearchQuery("Any").build())).isEqualTo(1);
  }

  @Test
  public void select_groups_by_query_with_global_permission() {
    GroupDto group1 = db.users().insertGroup(newGroupDto().setName("Group-1"));
    GroupDto group2 = db.users().insertGroup(newGroupDto().setName("Group-2"));
    GroupDto group3 = db.users().insertGroup(newGroupDto().setName("Group-3"));

    ComponentDto project = db.components().insertComponent(newProjectDto());

    db.users().insertPermissionOnAnyone(SCAN_EXECUTION);
    db.users().insertPermissionOnAnyone(PROVISIONING);
    db.users().insertPermissionOnGroup(group1, SCAN_EXECUTION);
    db.users().insertPermissionOnGroup(group3, SYSTEM_ADMIN);
    db.users().insertProjectPermissionOnGroup(group2, UserRole.ADMIN, project);

    assertThat(underTest.selectGroupNamesByPermissionQuery(dbSession,
      PermissionQuery.builder().setPermission(SCAN_EXECUTION).build())).containsExactly(ANYONE, "Group-1");

    assertThat(underTest.selectGroupNamesByPermissionQuery(dbSession,
      PermissionQuery.builder().setPermission(SYSTEM_ADMIN).build())).containsExactly("Group-3");

    assertThat(underTest.selectGroupNamesByPermissionQuery(dbSession,
      PermissionQuery.builder().setPermission(PROVISIONING).build())).containsExactly(ANYONE);
  }

  @Test
  public void select_groups_by_query_with_project_permissions() {
    GroupDto group1 = db.users().insertGroup(newGroupDto());
    GroupDto group2 = db.users().insertGroup(newGroupDto());
    GroupDto group3 = db.users().insertGroup(newGroupDto());

    ComponentDto project = db.components().insertComponent(newProjectDto());
    ComponentDto anotherProject = db.components().insertComponent(newProjectDto());

    db.users().insertProjectPermissionOnGroup(group1, SCAN_EXECUTION, project);
    db.users().insertProjectPermissionOnGroup(group1, PROVISIONING, project);
    db.users().insertProjectPermissionOnAnyone(USER, project);

    db.users().insertProjectPermissionOnGroup(group1, SYSTEM_ADMIN, anotherProject);
    db.users().insertProjectPermissionOnAnyone(SYSTEM_ADMIN, anotherProject);
    db.users().insertProjectPermissionOnGroup(group3, SCAN_EXECUTION, anotherProject);
    db.users().insertPermissionOnGroup(group2, SCAN_EXECUTION);

    PermissionQuery.Builder builderOnComponent = PermissionQuery.builder().setComponentUuid(project.uuid());
    assertThat(underTest.selectGroupNamesByPermissionQuery(dbSession,
      builderOnComponent.withAtLeastOnePermission().build())).containsOnlyOnce(group1.getName());
    assertThat(underTest.selectGroupNamesByPermissionQuery(dbSession,
      builderOnComponent.setPermission(SCAN_EXECUTION).build())).containsOnlyOnce(group1.getName());
    assertThat(underTest.selectGroupNamesByPermissionQuery(dbSession,
      builderOnComponent.setPermission(USER).build())).containsOnlyOnce(ANYONE);
  }

  @Test
  public void select_groups_by_query_paginated() {
    IntStream.rangeClosed(0, 9).forEach(i -> db.users().insertGroup(newGroupDto().setName(i + "-name")));

    assertThat(underTest.selectGroupNamesByPermissionQuery(dbSession,
      PermissionQuery.builder().setPageIndex(2).setPageSize(3).build())).containsExactly("3-name", "4-name", "5-name");
  }

  @Test
  public void select_groups_by_query_with_search_query() {
    GroupDto group = db.users().insertGroup(newGroupDto().setName("group-anyone"));
    db.users().insertGroup(newGroupDto().setName("unknown"));
    db.users().insertPermissionOnGroup(group, SCAN_EXECUTION);

    assertThat(underTest.selectGroupNamesByPermissionQuery(dbSession,
      PermissionQuery.builder().setSearchQuery("any").build())).containsOnlyOnce(ANYONE, "group-anyone");
  }

  @Test
  public void select_groups_by_query_does_not_return_anyone_when_group_roles_is_empty() {
    GroupDto group = db.users().insertGroup(newGroupDto());

    assertThat(underTest.selectGroupNamesByPermissionQuery(dbSession,
      PermissionQuery.builder().build()))
        .doesNotContain(ANYONE)
        .containsExactly(group.getName());
  }

  @Test
  public void select_group_permissions_by_group_names_on_global_permissions() {
    GroupDto group1 = db.users().insertGroup(newGroupDto().setName("Group-1"));
    db.users().insertPermissionOnGroup(group1, SCAN_EXECUTION);

    GroupDto group2 = db.users().insertGroup(newGroupDto().setName("Group-2"));
    ComponentDto project = db.components().insertComponent(newProjectDto());
    db.users().insertProjectPermissionOnGroup(group2, UserRole.ADMIN, project);

    GroupDto group3 = db.users().insertGroup(newGroupDto().setName("Group-3"));
    db.users().insertPermissionOnGroup(group3, SYSTEM_ADMIN);

    // Anyone
    db.users().insertPermissionOnAnyone(SCAN_EXECUTION);
    db.users().insertPermissionOnAnyone(PROVISIONING);

    assertThat(underTest.selectGroupPermissionsByGroupNamesAndProject(dbSession, asList("Group-1"), null))
      .extracting(GroupPermissionDto::getGroupId, GroupPermissionDto::getRole, GroupPermissionDto::getResourceId)
      .containsOnly(tuple(group1.getId(), SCAN_EXECUTION, null));

    assertThat(underTest.selectGroupPermissionsByGroupNamesAndProject(dbSession, asList("Group-2"), null)).isEmpty();

    assertThat(underTest.selectGroupPermissionsByGroupNamesAndProject(dbSession, asList("Group-3"), null))
      .extracting(GroupPermissionDto::getGroupId, GroupPermissionDto::getRole, GroupPermissionDto::getResourceId)
      .containsOnly(tuple(group3.getId(), SYSTEM_ADMIN, null));

    assertThat(underTest.selectGroupPermissionsByGroupNamesAndProject(dbSession, asList("Anyone"), null))
      .extracting(GroupPermissionDto::getGroupId, GroupPermissionDto::getRole, GroupPermissionDto::getResourceId)
      .containsOnly(
        tuple(0L, SCAN_EXECUTION, null),
        tuple(0L, PROVISIONING, null));

    assertThat(underTest.selectGroupPermissionsByGroupNamesAndProject(dbSession, asList("Group-1", "Group-2", "Anyone"), null)).hasSize(3);
    assertThat(underTest.selectGroupPermissionsByGroupNamesAndProject(dbSession, asList("Unknown"), null)).isEmpty();
    assertThat(underTest.selectGroupPermissionsByGroupNamesAndProject(dbSession, Collections.emptyList(), null)).isEmpty();
  }

  @Test
  public void select_group_permissions_by_group_names_on_project_permissions() {
    GroupDto group1 = db.users().insertGroup(newGroupDto().setName("Group-1"));
    db.users().insertPermissionOnGroup(group1, PROVISIONING);

    GroupDto group2 = db.users().insertGroup(newGroupDto().setName("Group-2"));
    ComponentDto project = db.components().insertComponent(newProjectDto());
    db.users().insertProjectPermissionOnGroup(group2, USER, project);

    GroupDto group3 = db.users().insertGroup(newGroupDto().setName("Group-3"));
    db.users().insertProjectPermissionOnGroup(group3, USER, project);

    // Anyone group
    db.users().insertPermissionOnAnyone(SCAN_EXECUTION);
    db.users().insertProjectPermissionOnAnyone(PROVISIONING, project);

    assertThat(underTest.selectGroupPermissionsByGroupNamesAndProject(dbSession, asList("Group-1"), project.getId())).isEmpty();

    assertThat(underTest.selectGroupPermissionsByGroupNamesAndProject(dbSession, asList("Group-2"), project.getId()))
      .extracting(GroupPermissionDto::getGroupId, GroupPermissionDto::getRole, GroupPermissionDto::getResourceId)
      .containsOnly(tuple(group2.getId(), USER, project.getId()));

    assertThat(underTest.selectGroupPermissionsByGroupNamesAndProject(dbSession, asList("Group-3"), project.getId()))
      .extracting(GroupPermissionDto::getGroupId, GroupPermissionDto::getRole, GroupPermissionDto::getResourceId)
      .containsOnly(tuple(group3.getId(), USER, project.getId()));

    assertThat(underTest.selectGroupPermissionsByGroupNamesAndProject(dbSession, asList("Anyone"), project.getId()))
      .extracting(GroupPermissionDto::getGroupId, GroupPermissionDto::getRole, GroupPermissionDto::getResourceId)
      .containsOnly(tuple(0L, PROVISIONING, project.getId()));

    assertThat(underTest.selectGroupPermissionsByGroupNamesAndProject(dbSession, asList("Group-1", "Group-2", "Anyone"), project.getId())).hasSize(2);
    assertThat(underTest.selectGroupPermissionsByGroupNamesAndProject(dbSession, asList("Unknown"), project.getId())).isEmpty();
    assertThat(underTest.selectGroupPermissionsByGroupNamesAndProject(dbSession, asList("Group-1"), 123L)).isEmpty();
    assertThat(underTest.selectGroupPermissionsByGroupNamesAndProject(dbSession, Collections.emptyList(), project.getId())).isEmpty();
  }

  @Test
  public void selectGlobalPermissionsOfGroup() {
    OrganizationDto org1 = db.organizations().insert();
    OrganizationDto org2 = db.organizations().insert();
    GroupDto group1 = db.users().insertGroup(org1, "group1");
    GroupDto group2 = db.users().insertGroup(org2, "group2");
    ComponentDto project = db.components().insertProject();

    db.users().insertPermissionOnAnyone(org1, "perm1");
    db.users().insertPermissionOnGroup(group1, "perm2");
    db.users().insertPermissionOnGroup(group1, "perm3");
    db.users().insertPermissionOnGroup(group2, "perm4");
    db.users().insertProjectPermissionOnGroup(group1, "perm5", project);
    db.users().insertProjectPermissionOnAnyone(org1, "perm6", project);

    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, org1.getUuid(), group1.getId())).containsOnly("perm2", "perm3");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, org2.getUuid(), group2.getId())).containsOnly("perm4");
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, org1.getUuid(), null)).containsOnly("perm1");

    // group1 is not in org2
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, org2.getUuid(), group1.getId())).isEmpty();
    assertThat(underTest.selectGlobalPermissionsOfGroup(dbSession, org2.getUuid(), null)).isEmpty();
  }

  @Test
  public void selectProjectPermissionsOfGroup() {
    OrganizationDto org1 = db.organizations().insert();
    GroupDto group1 = db.users().insertGroup(org1, "group1");
    ComponentDto project1 = db.components().insertProject();
    ComponentDto project2 = db.components().insertProject();

    db.users().insertPermissionOnAnyone(org1, "perm1");
    db.users().insertPermissionOnGroup(group1, "perm2");
    db.users().insertProjectPermissionOnGroup(group1, "perm3", project1);
    db.users().insertProjectPermissionOnGroup(group1, "perm4", project1);
    db.users().insertProjectPermissionOnGroup(group1, "perm5", project2);
    db.users().insertProjectPermissionOnAnyone(org1, "perm6", project1);

    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, org1.getUuid(), group1.getId(), project1.getId()))
      .containsOnly("perm3", "perm4");
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, org1.getUuid(), group1.getId(), project2.getId()))
      .containsOnly("perm5");
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, org1.getUuid(), null, project1.getId()))
      .containsOnly("perm6");
    assertThat(underTest.selectProjectPermissionsOfGroup(dbSession, org1.getUuid(), null, project2.getId()))
      .isEmpty();
  }

  @Test
  public void projectHasPermissions_is_false_if_no_permissions_at_all() {
    ComponentDto project1 = db.components().insertProject();
    db.users().insertPermissionOnAnyone("perm1");

    assertThat(underTest.hasRootComponentPermissions(dbSession, project1.getId())).isFalse();
  }

  @Test
  public void projectHasPermissions_is_true_if_at_least_one_permission_on_group() {
    GroupDto group1 = db.users().insertGroup(newGroupDto());
    ComponentDto project1 = db.components().insertProject();
    db.users().insertProjectPermissionOnGroup(group1, "perm1", project1);

    assertThat(underTest.hasRootComponentPermissions(dbSession, project1.getId())).isTrue();
  }

  @Test
  public void projectHasPermissions_is_true_if_at_least_one_permission_on_anyone() {
    ComponentDto project1 = db.components().insertProject();
    ComponentDto project2 = db.components().insertProject();
    db.users().insertProjectPermissionOnAnyone("perm1", project1);

    assertThat(underTest.hasRootComponentPermissions(dbSession, project1.getId())).isTrue();
    assertThat(underTest.hasRootComponentPermissions(dbSession, project2.getId())).isFalse();
  }

  @Test
  public void deleteByRootComponentId() {
    GroupDto group1 = db.users().insertGroup(newGroupDto());
    GroupDto group2 = db.users().insertGroup(newGroupDto());
    ComponentDto project1 = db.components().insertProject();
    ComponentDto project2 = db.components().insertProject();
    db.users().insertPermissionOnGroup(group1, "perm1");
    db.users().insertProjectPermissionOnGroup(group1, "perm2", project1);
    db.users().insertProjectPermissionOnAnyone("perm3", project1);
    db.users().insertProjectPermissionOnGroup(group2, "perm4", project2);

    underTest.deleteByRootComponentId(dbSession, project1.getId());
    dbSession.commit();

    assertThat(db.countSql("select count(id) from group_roles where resource_id=" + project1.getId())).isEqualTo(0);
    assertThat(db.countRowsOfTable("group_roles")).isEqualTo(2);
  }

  @Test
  public void delete_global_permission_from_group() {
    GroupDto group1 = db.users().insertGroup();
    ComponentDto project1 = db.components().insertProject();
    db.users().insertPermissionOnAnyone("perm1");
    db.users().insertPermissionOnGroup(group1, "perm2");
    db.users().insertProjectPermissionOnGroup(group1, "perm3", project1);
    db.users().insertProjectPermissionOnAnyone("perm4", project1);

    underTest.delete(dbSession, "perm2", group1.getOrganizationUuid(), group1.getId(), null);
    dbSession.commit();

    assertThatNoPermission("perm2");
    assertThat(db.countRowsOfTable("group_roles")).isEqualTo(3);
  }

  @Test
  public void delete_global_permission_from_anyone() {
    GroupDto group1 = db.users().insertGroup();
    ComponentDto project1 = db.components().insertProject();
    db.users().insertPermissionOnAnyone("perm1");
    db.users().insertPermissionOnGroup(group1, "perm2");
    db.users().insertProjectPermissionOnGroup(group1, "perm3", project1);
    db.users().insertProjectPermissionOnAnyone("perm4", project1);

    underTest.delete(dbSession, "perm1", group1.getOrganizationUuid(), null, null);
    dbSession.commit();

    assertThatNoPermission("perm1");
    assertThat(db.countRowsOfTable("group_roles")).isEqualTo(3);
  }

  @Test
  public void delete_project_permission_from_group() {
    GroupDto group1 = db.users().insertGroup();
    ComponentDto project1 = db.components().insertProject();
    db.users().insertPermissionOnAnyone("perm1");
    db.users().insertPermissionOnGroup(group1, "perm2");
    db.users().insertProjectPermissionOnGroup(group1, "perm3", project1);
    db.users().insertProjectPermissionOnAnyone("perm4", project1);

    underTest.delete(dbSession, "perm3", group1.getOrganizationUuid(), group1.getId(), project1.getId());
    dbSession.commit();

    assertThatNoPermission("perm3");
    assertThat(db.countRowsOfTable("group_roles")).isEqualTo(3);
  }

  @Test
  public void delete_project_permission_from_anybody() {
    GroupDto group1 = db.users().insertGroup();
    ComponentDto project1 = db.components().insertProject();
    db.users().insertPermissionOnAnyone("perm1");
    db.users().insertPermissionOnGroup(group1, "perm2");
    db.users().insertProjectPermissionOnGroup(group1, "perm3", project1);
    db.users().insertProjectPermissionOnAnyone("perm4", project1);

    underTest.delete(dbSession, "perm4", group1.getOrganizationUuid(), null, project1.getId());
    dbSession.commit();

    assertThatNoPermission("perm4");
    assertThat(db.countRowsOfTable("group_roles")).isEqualTo(3);
  }

  private void assertThatNoPermission(String permission) {
    assertThat(db.countSql("select count(id) from group_roles where role='" + permission + "'")).isEqualTo(0);
  }
}
