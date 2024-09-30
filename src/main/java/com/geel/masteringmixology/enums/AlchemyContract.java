package com.geel.masteringmixology.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
public class AlchemyContract {
    @Getter
    private AlchemyPotion potion;

    @Getter
    private AlchemyBuilding type;

    @Getter
    private ContractState state;

    public void reset() {
        potion = AlchemyPotion.NONE;
        type = AlchemyBuilding.NONE;
        state = ContractState.NONE;
    }

    public void setPotion(AlchemyPotion potion) {
        this.potion = potion;
        this.state = ContractState.NONE;

    }

    public void setType(AlchemyBuilding type) {
        this.type = type;
        this.state = ContractState.NONE;
    }

    public boolean isCompleteRecipe() {
        return potion != AlchemyPotion.NONE && type != AlchemyBuilding.NONE;
    }

    public void select() {
        if (this.state == ContractState.NONE || this.state == ContractState.EXCLUDED) {
            this.state = ContractState.SHOULD_MAKE_BASE;
        }
    }

    public void exclude() {
        if (this.state == ContractState.NONE || this.state == ContractState.SELECTED) {
            this.state = ContractState.EXCLUDED;
        }
    }

    public boolean consumeBasePotionInMixerEvent(AlchemyPotion potion) {
        if (this.state == ContractState.SHOULD_MAKE_BASE && potion == this.potion) {
            this.state = ContractState.BASE_IN_MIXER;
            return true;
        }

        return false;
    }

    public boolean consumeBasePotionNoLongerInMixerEvent() {
        if (this.state == ContractState.BASE_IN_MIXER) {
            this.state = ContractState.SHOULD_MAKE_BASE;
            return true;
        }

        return false;
    }

    public boolean consumeBasePotionAddedToInventoryEvent(AlchemyPotion potion) {
        if (this.state == ContractState.BASE_IN_MIXER && potion == this.potion) {
            this.state = ContractState.SHOULD_PROCESS_BASE;
            return true;
        }

        return false;
    }

    public boolean consumeBuildingProcessingStarted(AlchemyPotion potion, AlchemyBuilding building) {
        if (this.state == ContractState.SHOULD_PROCESS_BASE && potion == this.potion && building == this.type) {
            this.state = ContractState.PROCESSING_BASE;
            return true;
        }

        return false;
    }

    public boolean consumeBuildingProcessingFinished(AlchemyPotion potion, AlchemyBuilding building) {
        if (this.state == ContractState.PROCESSING_BASE && potion == this.potion && building == this.type) {
            this.state = ContractState.READY;
            return true;
        }

        return false;
    }
}
