---
title: Basics
---
This page explains the few terms that appear all over the handbook. You need nothing more to get started.

## Ticks and seconds

Minecraft counts in **ticks (t)**. One second has 20 ticks. If a machine needs "200 t (10 s)", one operation takes ten seconds.

## Power: J and J/t

- Power is measured in **joules (J)**.
- **J/t** means "joules per tick". A generator with 20 J/t produces 20 J every tick; a machine with 20 J/t uses that much while it works.
- Power flows through **cables** from generators to machines. Every cable has a **throughput**: the most J/t it can carry.
- Every machine has a small **buffer**. If power is short for a moment, it pauses and then carries on. Nothing is lost.

More under [[strom|Power]].

## Fluids: mB

Liquids and gases are called **fluids**. They are measured in **millibuckets (mB)**: 1,000 mB are exactly one bucket. Fluids travel through **pipes** and are stored in **tanks**. More under [[fluide|Fluids and Pipes]].

## The status lamp

Every machine has a small lamp:

- **Green:** working.
- **Yellow:** waiting for material or power.
- **Red:** problem. The output is full, a structure is incomplete or the machine stands in the wrong place. In the window, the lamp's tooltip tells you the reason.

## Inputs and outputs

- Machines accept items and fluids **from all sides**. You can connect hoppers and pipes wherever you like.
- Machines and storage blocks output fluids and stored power through **one side** only, the **output side**. You choose it in the window with the "Side" button.

## Multiblocks

Some machines are so big that they consist of many blocks, for example the [[raffinerie|Refinery]]. You place a **controller** and build the given shape around it from **casing blocks**. **Ports** replace single casing blocks and are the connections for pipes, cables and hoppers.

## Phases

Infinite Horizon is split into six **phases**, from industry on Earth to the starship. Each phase has its own chapter in the handbook with a "First Steps" page. From phase 2 on, you unlock new technology through **research**.

@tip If you remember only one thing: **green works, yellow waits, red has a problem.** One look at the lamps shows you where your factory is stuck.
