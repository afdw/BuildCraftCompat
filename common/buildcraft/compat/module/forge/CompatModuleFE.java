package buildcraft.compat.module.forge;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.IMjReadable;
import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjAPI;

import buildcraft.lib.misc.MathUtil;

import buildcraft.compat.CompatModuleBase;

public class CompatModuleFE extends CompatModuleBase {
    private static boolean checking = false;

    @Override
    public String compatModId() {
        return "forge";
    }

    @Override
    public void preInit() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    private static long feToMj(int fe) {
        return (long) fe * MjAPI.MJ / 10;
    }

    private static int mjToFe(long mj) {
        return (int) MathUtil.clamp(mj / MjAPI.MJ * 10, 0L, (long) Integer.MAX_VALUE);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onAttachCapabilitiesTileEntity(AttachCapabilitiesEvent<TileEntity> event) {
        event.addCapability(
            new ResourceLocation("buildcraftcompat", "mj"),
            new ICapabilityProvider() {
                @Override
                public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
                    if (checking) {
                        return false;
                    }
                    checking = true;
                    boolean result = capability == CapabilityEnergy.ENERGY &&
                        event.getObject().hasCapability(MjAPI.CAP_CONNECTOR, facing);
                    checking = false;
                    return result;
                }

                @Nullable
                @Override
                public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
                    if (checking) {
                        return null;
                    }
                    checking = true;
                    T result = null;
                    if (capability == CapabilityEnergy.ENERGY &&
                        event.getObject().hasCapability(MjAPI.CAP_CONNECTOR, facing)) {
                        result = CapabilityEnergy.ENERGY.cast(new IEnergyStorage() {
                            @Override
                            public int receiveEnergy(int maxReceive, boolean simulate) {
                                return event.getObject().hasCapability(MjAPI.CAP_RECEIVER, facing)
                                    ?
                                    mjToFe(
                                        Objects.requireNonNull(event.getObject().getCapability(MjAPI.CAP_RECEIVER, facing))
                                            .receivePower(
                                                feToMj(maxReceive),
                                                simulate
                                            )
                                    )
                                    : 0;
                            }

                            @Override
                            public int extractEnergy(int maxExtract, boolean simulate) {
                                return 0;
                            }

                            @Override
                            public int getEnergyStored() {
                                return event.getObject().hasCapability(MjAPI.CAP_READABLE, facing)
                                    ?
                                    mjToFe(
                                        Objects.requireNonNull(event.getObject().getCapability(MjAPI.CAP_READABLE, facing))
                                            .getStored()
                                    )
                                    : 0;
                            }

                            @Override
                            public int getMaxEnergyStored() {
                                return event.getObject().hasCapability(MjAPI.CAP_READABLE, facing)
                                    ?
                                    mjToFe(
                                        Objects.requireNonNull(event.getObject().getCapability(MjAPI.CAP_READABLE, facing))
                                            .getCapacity()
                                    )
                                    : 0;
                            }

                            @Override
                            public boolean canExtract() {
                                return false;
                            }

                            @Override
                            public boolean canReceive() {
                                return event.getObject().hasCapability(MjAPI.CAP_RECEIVER, facing);
                            }
                        });
                    }
                    checking = false;
                    return result;
                }
            }
        );
        event.addCapability(
            new ResourceLocation("buildcraftcompat", "fe"),
            new ICapabilityProvider() {
                @Override
                public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
                    if (checking) {
                        return false;
                    }
                    checking = true;
                    boolean result = capability == MjAPI.CAP_CONNECTOR &&
                        event.getObject().hasCapability(CapabilityEnergy.ENERGY, facing);
                    checking = false;
                    return result;
                }

                @Nullable
                @Override
                public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
                    if (checking) {
                        return null;
                    }
                    checking = true;
                    T result = null;
                    if (event.getObject().hasCapability(CapabilityEnergy.ENERGY, facing)) {
                        if (capability == MjAPI.CAP_CONNECTOR) {
                            result = MjAPI.CAP_CONNECTOR.cast(other -> true);
                        }
                        if (capability == MjAPI.CAP_READABLE) {
                            result = MjAPI.CAP_READABLE.cast(new IMjReadable() {
                                @Override
                                public long getStored() {
                                    return mjToFe(
                                        Objects.requireNonNull(event.getObject().getCapability(CapabilityEnergy.ENERGY, facing))
                                            .getEnergyStored()
                                    );
                                }

                                @Override
                                public long getCapacity() {
                                    return mjToFe(
                                        Objects.requireNonNull(event.getObject().getCapability(CapabilityEnergy.ENERGY, facing))
                                            .getMaxEnergyStored()
                                    );
                                }

                                @Override
                                public boolean canConnect(@Nonnull IMjConnector other) {
                                    return Objects.requireNonNull(getCapability(MjAPI.CAP_CONNECTOR, facing))
                                        .canConnect(other);
                                }
                            });
                        }
                        if (capability == MjAPI.CAP_RECEIVER) {
                            result = MjAPI.CAP_RECEIVER.cast(new IMjReceiver() {
                                @Override
                                public long getPowerRequested() {
                                    return feToMj(
                                        Objects.requireNonNull(event.getObject().getCapability(CapabilityEnergy.ENERGY, facing))
                                            .receiveEnergy(Integer.MAX_VALUE, true)
                                    );
                                }

                                @Override
                                public long receivePower(long microJoules, boolean simulate) {
                                    return feToMj(
                                        Objects.requireNonNull(event.getObject().getCapability(CapabilityEnergy.ENERGY, facing))
                                            .receiveEnergy(mjToFe(microJoules), simulate)
                                    );
                                }

                                @Override
                                public boolean canConnect(@Nonnull IMjConnector other) {
                                    return Objects.requireNonNull(getCapability(MjAPI.CAP_CONNECTOR, facing))
                                        .canConnect(other);
                                }
                            });
                        }
                    }
                    checking = false;
                    return result;
                }
            }
        );
    }
}
