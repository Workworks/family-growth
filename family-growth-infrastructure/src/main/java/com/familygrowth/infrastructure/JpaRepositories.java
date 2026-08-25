package com.familygrowth.infrastructure;
import java.util.UUID; import org.springframework.data.jpa.repository.JpaRepository;
interface FamilyJpaRepository extends JpaRepository<FamilyEntity,UUID>{} interface ParentJpaRepository extends JpaRepository<ParentEntity,UUID>{} interface ChildJpaRepository extends JpaRepository<ChildEntity,UUID>{} interface PlanJpaRepository extends JpaRepository<PlanEntity,UUID>{} interface GoalJpaRepository extends JpaRepository<GoalEntity,UUID>{} interface TaskJpaRepository extends JpaRepository<TaskEntity,UUID>{}
