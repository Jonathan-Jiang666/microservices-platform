package com.tech.microservices.inventory_service.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.tech.microservices.inventory_service.dto.command.InventoryReleaseRequestCommand;
import com.tech.microservices.inventory_service.dto.command.InventoryReservationRequestCommand;
import com.tech.microservices.inventory_service.exception.DuplicateCommandEventException;
import com.tech.microservices.inventory_service.exception.InventoryReleaseCommandFailureException;
import com.tech.microservices.inventory_service.exception.InventoryReservationCommandFailureException;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor
public class InventoryCommandProcessor {
     private final InventoryCommandService commandService;
     private final InventoryFailureEventService failureEventService;

    public void processReservation(
            UUID eventId,
            InventoryReservationRequestCommand command
    ) {
        try{
        try {
            commandService.reserveOrderInventory(
                    eventId,
                    command
            );
        } catch (InventoryReservationCommandFailureException failure) {
            failureEventService.recordFailure(
                    eventId,
                    command,
                    failure
            );
        }
        }catch(DuplicateCommandEventException duplicate)
        {
            // 如果是重复的事件，直接忽略
            return ;
        }
    }

    public void processOrderCancelled(
            UUID eventId,
            InventoryReleaseRequestCommand command
    ) {
        try{
            
        try{
             commandService.cancelOrderInventory(
                eventId,
                command
        );

        }catch(InventoryReleaseCommandFailureException failure)
        {
            failureEventService.recordReleaseFailure(
                    eventId,
                    command,
                    failure
            );
        }
    }catch(DuplicateCommandEventException duplicate)
    {
        // 如果是重复的事件，直接忽略
        return ;
       
    }
    }

}
