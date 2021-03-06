package cn.jmicro.choreography.assign;

import cn.jmicro.common.CommonException;
import cn.jmicro.common.util.StringUtils;

public class Assign {

	
	public Assign(String depId,String agentId,String insId) {
		if(StringUtils.isEmpty(insId)) {
			throw new CommonException("Process instance ID cannot be NULL");
		}
		
		if(StringUtils.isEmpty(agentId)) {
			throw new CommonException("Agent ID cannot be NULL");
		}
		
		if(StringUtils.isEmpty(depId)) {
			throw new CommonException("Deployment ID cannot be NULL");
		}
		
		this.depId = depId;
		this.agentId = agentId;
		this.insId = insId;
	}
	
	private String depId;
	private String agentId;
	private String insId;
	
	public AssignState state = AssignState.INIT;
	
	public long opTime;
	
	public int checkTime = 0;
	
	public String getDepId() {
		return depId;
	}

	public String getAgentId() {
		return agentId;
	}

	public String getInsId() {
		return insId;
	}

	@Override
	public int hashCode() {
		if(this.getInsId() == null || "".equals(this.getInsId())) {
			return (this.agentId + this.depId).hashCode();
		} else {
			return Integer.parseInt(this.getInsId());
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		return hashCode() == obj.hashCode();
	}

	@Override
	public String toString() {
		return "Assign [depId=" + depId + ", agentId=" + agentId + ", insId=" + insId + ", state=" + state + ", opTime="
				+ opTime + ", checkTime=" + checkTime + "]";
	}

}
