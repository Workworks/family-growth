package com.familygrowth.infrastructure;
import jakarta.persistence.*; import java.time.*; import java.util.UUID;
@MappedSuperclass abstract class BaseEntity { @Id UUID id; @Version long version; Instant createdAt; }
@Entity @Table(name="family") class FamilyEntity extends BaseEntity { String name; }
@Entity @Table(name="parent_profile") class ParentEntity extends BaseEntity { UUID familyId; String displayName; }
@Entity @Table(name="child_profile") class ChildEntity extends BaseEntity { UUID familyId; String displayName; LocalDate birthDate; String ageStage; }
@Entity @Table(name="growth_plan") class PlanEntity extends BaseEntity { UUID familyId; UUID childId; String title; String description; LocalDate startDate; LocalDate endDate; boolean active; }
@Entity @Table(name="growth_goal") class GoalEntity extends BaseEntity { UUID familyId; UUID planId; String title; String description; }
@Entity @Table(name="growth_task") class TaskEntity extends BaseEntity { UUID familyId; UUID goalId; String title; String description; String category; String difficulty; int expectedMinutes; boolean active; }
