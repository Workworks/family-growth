package com.familygrowth.infrastructure;
import com.familygrowth.application.FamilyGrowthService;import com.familygrowth.application.Stage8Store;import com.familygrowth.domain.Stage3Models.Wallet;import com.familygrowth.domain.Stage8Models.*;import java.time.*;import java.util.*;import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.stereotype.Repository;
@Repository class JdbcStage8Store implements Stage8Store{
 private final JdbcTemplate jdbc;JdbcStage8Store(JdbcTemplate jdbc){this.jdbc=jdbc;}
 @Override public ChildSyncSnapshot snapshot(UUID family,UUID child,Instant now){
  String name=jdbc.query("SELECT display_name FROM child_profile WHERE family_id=? AND id=?",(r,n)->r.getString(1),family,child).stream().findFirst().orElseThrow(FamilyGrowthService.NotFoundException::new);
  Wallet wallet=jdbc.query("SELECT child_id,family_id,money_balance,reserved_money,coin_balance,version FROM wallet WHERE family_id=? AND child_id=?",(r,n)->{var money=r.getBigDecimal("money_balance");var reserved=r.getBigDecimal("reserved_money");return new Wallet(r.getObject("child_id",UUID.class),r.getObject("family_id",UUID.class),money,reserved,money.subtract(reserved),r.getLong("coin_balance"),r.getLong("version"));},family,child).stream().findFirst().orElseThrow(FamilyGrowthService.NotFoundException::new);
  List<SyncTask> tasks=jdbc.query("""
   SELECT t.id,t.title,t.expected_minutes,
   COALESCE((SELECT tc.status FROM task_completion tc WHERE tc.family_id=t.family_id AND tc.child_id=? AND tc.task_id=t.id ORDER BY tc.submitted_at DESC LIMIT 1),'TODO') AS task_status,
   (SELECT tc.id FROM task_completion tc WHERE tc.family_id=t.family_id AND tc.child_id=? AND tc.task_id=t.id ORDER BY tc.submitted_at DESC LIMIT 1) AS completion_id
   FROM growth_task t JOIN growth_goal g ON g.id=t.goal_id JOIN growth_plan p ON p.id=g.plan_id
   WHERE t.family_id=? AND p.child_id=? AND t.active=TRUE ORDER BY t.created_at,t.id
   """,(r,n)->new SyncTask(r.getObject("id",UUID.class),r.getString("title"),r.getInt("expected_minutes"),r.getString("task_status"),r.getObject("completion_id",UUID.class)),child,child,family,child);
  Integer pending=jdbc.queryForObject("SELECT COUNT(*) FROM task_completion WHERE family_id=? AND child_id=? AND status='SUBMITTED'",Integer.class,family,child);
  Instant start=LocalDate.ofInstant(now,ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
  Integer approved=jdbc.queryForObject("SELECT COUNT(*) FROM task_completion WHERE family_id=? AND child_id=? AND status='APPROVED' AND reviewed_at>=?",Integer.class,family,child,java.sql.Timestamp.from(start));
  return new ChildSyncSnapshot(family,child,name,wallet,tasks,pending==null?0:pending,approved==null?0:approved,now);
 }
}
