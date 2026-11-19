---
title: Using Machines
---
All Infinite Horizon machines follow the same rules. Once you understand them, you can handle every new machine right away.

## The window

Right-clicking a machine opens its window:

- **Far left** the energy bar: how much power the machine has stored.
- **Left** the input slots, **right** the output slots, the progress arrow in between.
- **Far right** the fluid bars, if the machine has tanks. Below them are the fill and drain slots for buckets.
- **Bottom right** the two upgrade slots (see [[upgrades|Upgrades]]).
- The **"Side"** button chooses on which side the machine outputs fluids. With several output tanks, the **"Tank"** button chooses which one fills the bucket.

## The status lamp

Every machine has a small lamp on top. It tells you at a glance what is going on:

- **Green** – the machine is working.
- **Yellow** – it is waiting for material or power.
- **Red** – something is wrong: the output is full, the machine stands in the wrong place or a structure is incomplete. Hover over the lamp in the window to see the reason.

## Automating with hoppers and pipes

- Items and fluids enter the input **from every side**. Hoppers always fill the first matching slot.
- From every side you can extract **only the results**. That makes chaining machines easy.
- A machine outputs fluids only through its **output side** (button "Side"). That side accepts nothing itself, so nothing flows back into the pipe that feeds the machine.
- Shift-clicking in the window puts upgrades into the upgrade slots and molds into the mold slot, everything else into the input.

## Breaking

Break machines with a **pickaxe** (stone or better). They drop themselves, and items inside fall out. Energy and fluids are lost. Only the <item:infinite_horizon:fluid_tank> keeps its contents.

@tip A machine uses power only while it is actually working. A waiting machine costs nothing, so feel free to hook several machines to one generator.
