package com.github.simiacryptus.aicoder.openai;

public class Engine {
  public String id;
  public boolean ready;
  public String owner;
  public String object;
  public Integer created;
  public String permissions;
  public Integer replicas;
  public Integer max_replicas;

  public Engine() {
  }

  public Engine(String id, boolean ready, String owner, String object, Integer created, String permissions, Integer replicas, Integer max_replicas) {
    this.id = id;
    this.ready = ready;
    this.owner = owner;
    this.object = object;
    this.created = created;
    this.permissions = permissions;
    this.replicas = replicas;
    this.max_replicas = max_replicas;
  }

}
