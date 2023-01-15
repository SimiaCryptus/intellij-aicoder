package com.github.simiacryptus.aicoder.openai;

public class Engine {
    @SuppressWarnings("unused")
    public String id;
    @SuppressWarnings("unused")
    public boolean ready;
    @SuppressWarnings("unused")
    public String owner;
    @SuppressWarnings("unused")
    public String object;
    @SuppressWarnings("unused")
    public Integer created;
    @SuppressWarnings("unused")
    public String permissions;
    @SuppressWarnings("unused")
    public Integer replicas;
    @SuppressWarnings("unused")
    public Integer max_replicas;

    @SuppressWarnings("unused")
    public Engine() {
    }

    @SuppressWarnings("unused")
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
