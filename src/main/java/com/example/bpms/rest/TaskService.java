package com.example.bpms.rest;

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.jbpm.services.task.audit.impl.model.BAMTaskSummaryImpl;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.TaskSummary;
import org.kie.remote.client.api.RemoteRestRuntimeEngineFactory;
import org.kie.remote.client.api.RemoteRuntimeEngineFactory;
import org.kie.remote.client.api.exception.InsufficientInfoToBuildException;

@Path("/task-service")
public class TaskService {

	@PersistenceContext
	EntityManager em;

	private final RemoteRestRuntimeEngineFactory factory;

	public TaskService() throws InsufficientInfoToBuildException,
			MalformedURLException {
		factory = RemoteRuntimeEngineFactory.newRestBuilder()
				.addUrl(new URL("http://localhost:8080/business-central"))
				.addDeploymentId("org.kie.example:project1:1.0.1")
				.addUserName("bpmsAdmin").addPassword("jbossadmin1!")
				.buildFactory();
	}
	
	@GET
	@Path("/{uid}/tasklist")
	@Produces("application/json")
	public List<TaskSummary> getTasksForUser(@PathParam("uid") final String uid) {
		return factory.newRuntimeEngine().getTaskService()
				.getTasksAssignedAsPotentialOwner(uid, null);
	}

	@GET
	@Path("/{uid}/summary")
	@Produces("application/json")
	public DashboardSummary getSummary(@PathParam("uid") final String uid) {
		TypedQuery<BAMTaskSummaryImpl> query = em.createQuery(
				"FROM BAMTaskSummaryImpl", BAMTaskSummaryImpl.class);
		List<BAMTaskSummaryImpl> bamTasks = query.getResultList();
		List<TaskSummary> runtimeTasks = factory.newRuntimeEngine()
				.getTaskService().getTasksAssignedAsPotentialOwner(uid, null);

		return new DashboardSummary(
				getTotalTasks(bamTasks), 
				getMyTasks(bamTasks, uid), 
				getUrgentTasks(runtimeTasks),
				getTotalCompleted(bamTasks),
				getAverageTimeToCompletion(bamTasks));
	}

	private long getTotalTasks(List<BAMTaskSummaryImpl> bamTasks) {
		return bamTasks.size();
	}

	private long getMyTasks(List<BAMTaskSummaryImpl> bamTasks, String uid) {
		Predicate<BAMTaskSummaryImpl> myTasks = (t -> Objects.equals(uid,
				t.getUserId()));
		return bamTasks.stream().filter(myTasks).count();
	}

	private long getUrgentTasks(List<TaskSummary> runtimeTasks) {
		Function<TaskSummary, Date> toExpirationTime = (ts -> ts
				.getExpirationTime());
		Predicate<Date> dueTwoDaysOrNoDueDate = d -> d == null
				|| LocalDate
						.now()
						.plusDays(2)
						.isAfter(
								d.toInstant().atZone(ZoneId.systemDefault())
										.toLocalDate());

		return runtimeTasks.stream().map(toExpirationTime)
				.filter(dueTwoDaysOrNoDueDate).count();
	}

	private long getTotalCompleted(List<BAMTaskSummaryImpl> bamTasks) {
		Predicate<BAMTaskSummaryImpl> totalCompleted = (t -> Status.Completed
				.toString().equals(t.getStatus()));
		return bamTasks.stream().filter(totalCompleted).count();
	}

	private String getAverageTimeToCompletion(List<BAMTaskSummaryImpl> bamTasks) {
		Function<BAMTaskSummaryImpl, Duration> timeToCompletion = (task -> {
			try {
				Field createdDate = BAMTaskSummaryImpl.class.getDeclaredField("createdDate");
				createdDate.setAccessible(true);
				Date start = (Date) createdDate.get(task);
				Date end = task.getEndDate();
				return Duration.between(start.toInstant(), end.toInstant());
			} catch (IllegalArgumentException | IllegalAccessException
					| NoSuchFieldException | SecurityException e) {
				throw new IllegalStateException("could not retrieve createdDate for task summary. cannot calculate average",e);
			}
		});

		Duration averageTimeToCompletion = bamTasks
				.stream()
				.filter(t -> t.getEndDate() != null)
				.map(timeToCompletion)
				.collect(() -> new DurationAverage(), DurationAverage::add,
						DurationAverage::combine).compute();

		long hours = averageTimeToCompletion.toHours();
		long minutes = averageTimeToCompletion.minusHours(hours).toMinutes();
		
		return hours + "h " + minutes + "m";

	}

	public class DurationAverage {
		public Duration runningSum = Duration.ZERO;
		public long count = 0;

		void add(Duration d) {
			runningSum = runningSum.plus(d);
			count++;
		}

		void combine(DurationAverage da) {
			runningSum = runningSum.plus(da.runningSum);
			count += da.count;
		}

		Duration compute() {
			return count != 0 ? runningSum.dividedBy(count) : Duration.ZERO;
		}
	}

	public class DashboardSummary {
		private long totalTasks; // total tasks for user and group
		private long myTasks; // total tasks for user
		private long urgentTasks; // tasks due today
		private long totalCompleted; // total tasks completed by user
		private String avgTimeToCompletion;

		public DashboardSummary() {
		}

		public DashboardSummary(long totalTasks, long myTasks,
				long urgentTasks, long totalCompleted,
				String avgTimeToCompletion) {
			super();
			this.totalTasks = totalTasks;
			this.myTasks = myTasks;
			this.urgentTasks = urgentTasks;
			this.totalCompleted = totalCompleted;
			this.avgTimeToCompletion = avgTimeToCompletion;
		}

		public long getTotalTasks() {
			return totalTasks;
		}

		public void setTotalTasks(long totalTasks) {
			this.totalTasks = totalTasks;
		}

		public long getMyTasks() {
			return myTasks;
		}

		public void setMyTasks(long myTasks) {
			this.myTasks = myTasks;
		}

		public long getUrgentTasks() {
			return urgentTasks;
		}

		public void setUrgentTasks(long urgentTasks) {
			this.urgentTasks = urgentTasks;
		}

		public long getTotalCompleted() {
			return totalCompleted;
		}

		public void setTotalCompleted(long totalCompleted) {
			this.totalCompleted = totalCompleted;
		}

		public String getAvgTimeToCompletion() {
			return avgTimeToCompletion;
		}

		public void setAvgTimeToCompletion(String avgTimeToCompletion) {
			this.avgTimeToCompletion = avgTimeToCompletion;
		}

	}

}
