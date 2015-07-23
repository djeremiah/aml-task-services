package com.example.bpms.rest;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.kie.api.task.model.Status;
import org.kie.api.task.model.TaskSummary;
import org.kie.remote.client.api.RemoteRestRuntimeEngineFactory;
import org.kie.remote.client.api.RemoteRuntimeEngineFactory;
import org.kie.remote.client.api.exception.InsufficientInfoToBuildException;
import org.kie.services.client.api.command.RemoteRuntimeEngine;

@Path("/task-service")
public class TaskService {

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
		RemoteRuntimeEngine runtimeEngine = factory.newRuntimeEngine();
		org.kie.api.task.TaskService taskService = runtimeEngine.getTaskService();
		List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwnerByStatus(uid, Arrays.asList(Status.values()), null);
		
		return new DashboardSummary(
				tasks.size(), 
				0, 
				0, 
				tasks.stream().filter(t -> Status.Completed.equals(t.getStatus())).count(), 
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
		private long totalTasks;
		private long myTasks;
		private long urgentTasks;
		private long totalCompleted;
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
