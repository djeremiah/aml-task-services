package com.example.bpms.rest;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
				.addUserName("bpmsAdmin")
				.addPassword("jbossadmin1!")
				.buildFactory();
	}

	@GET
	@Path("/{uid}/summary")
	@Produces("application/json")
	public DashboardSummary getSummary(@PathParam("uid") final String uid) {
		TypedQuery<BAMTaskSummaryImpl> query = em.createQuery("FROM BAMTaskSummaryImpl", BAMTaskSummaryImpl.class);
		List<BAMTaskSummaryImpl> bamTasks = query.getResultList();
		
		List<String> gids = new ArrayList<>();
		Predicate<BAMTaskSummaryImpl> myTasks = (t -> Objects.equals(uid, t.getUserId()));
		Predicate<BAMTaskSummaryImpl> totalCompleted = (t -> Status.Completed.toString().equals(t.getStatus()));
		
		return new DashboardSummary(
				0, 
				bamTasks.stream().filter(myTasks).count(), 
				factory.newRuntimeEngine().getTaskService().getTasksAssignedAsPotentialOwner(uid, null).stream().map(ts ->  ts.getExpirationTime()).filter(d -> d == null || LocalTime.now().plusHours(48).isAfter(LocalDateTime.ofInstant(d.toInstant(), null).toLocalTime())).count(), 
				bamTasks.stream().filter(totalCompleted).count(), 
				"NEVER");
	}
	
	

	@GET
	@Path("/{uid}/tasklist")
	@Produces("application/json")
	public List<TaskSummary> getTasksForUser(@PathParam("uid") final String uid) {
		return factory.newRuntimeEngine().getTaskService()
				.getTasksAssignedAsPotentialOwner(uid, null);
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
