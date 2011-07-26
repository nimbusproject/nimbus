package org.globus.workspace.service.impls.async;


import org.globus.workspace.Lager;
import org.globus.workspace.TempLocator;
import org.globus.workspace.scheduler.defaults.ResourcepoolEntry;

public class VMMRequestContext {

    private final int id;
    private final String name;
    private final Lager lager;
    private ResourcepoolEntry vmm;

    public ResourcepoolEntry getVmm() {
        return this.vmm;
    }

    public VMMRequestContext(int id, String name, Lager lagerImpl) {

        this.id = id;
        this.name = name;

        if (lagerImpl == null) {
            throw new IllegalArgumentException("lagerImpl may not be null");
        }
        this.lager = lagerImpl;
    }

    public void setVm(ResourcepoolEntry vmm) {
        this.vmm = vmm;
    }

    public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public Lager lager() {
        return this.lager;
    }
}
