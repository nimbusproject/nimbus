package org.globus.workspace.service.impls.async;


import org.globus.workspace.Lager;
import org.globus.workspace.TempLocator;
import org.globus.workspace.scheduler.defaults.ResourcepoolEntry;

public class VMMRequestContext {

    private final int id;
    private final String name;
    private final TempLocator locator;
    private final Lager lager;
    private ResourcepoolEntry vmm;

    public VMMRequestContext(int id,
                                   String name,
                                   TempLocator aLocator,
                                   Lager lagerImpl) {

        this.id = id;
        this.name = name;

        if (aLocator == null) {
            throw new IllegalArgumentException("aLocator may not be null");
        }
        this.locator = aLocator;

        if (lagerImpl == null) {
            throw new IllegalArgumentException("lagerImpl may not be null");
        }
        this.lager = lagerImpl;
    }


    public ResourcepoolEntry getVmm() {
        return this.vmm;
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

    public TempLocator getLocator() {
        return this.locator;
    }

    public Lager lager() {
        return this.lager;
    }
}
