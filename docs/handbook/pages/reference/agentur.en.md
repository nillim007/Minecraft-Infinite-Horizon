---
title: Your Agency
---
Every player belongs to an **agency**. It is your space program: all members share research points and research, later also atlas, routes and stations. If you play alone, you are your own agency.

## How it comes about

When you enter the world, the server checks whether you belong to an agency. If not, it founds one for you, and you are its **leader**. That also applies to worlds that existed before this version. As long as the agency has no name of its own, it is called "<your name>'s Agency".

The agency also remembers every dimension one of its members has entered. That counts for research: when a member sets foot on the Moon, tier 2 opens (see [[forschung|Research]]).

## Ownership

If you place a machine, a generator, a storage block or a port, it belongs to your agency. Cables, pipes and casing blocks have no owner.

Blocks not placed by a player (by command, from a dispenser or from older worlds) have **no owner**. Anyone may use them. Whoever opens their GUI first becomes their owner with their agency.

## Joining and leaving

Everything happens at the <item:infinite_horizon:research_terminal> in the **Agency** tab. There you see the members with their roles.

1. **Invite:** the leader sees on the right all players who are online and not members, and invites them with a button. The invited player gets a chat message.
2. **Accept or decline:** your open invitations are at the bottom right. You can accept at **any** research terminal, even someone else's.
3. **Joining:** you leave your old agency and become an engineer of the new one. If the old one becomes empty, all its property (machines, domes, terminals) belongs to the new one from then on. Research points and research of the old agency are **not** transferred.
4. **Leaving:** you immediately found a new agency of your own and are its leader. You cannot leave if you are alone.

If the leader leaves, the engineer who has been there longest becomes leader.

@warning Whoever joins another agency does not take their research points and research along. The new agency's progress applies.

## Rights

| Role | May |
| --- | --- |
| Leader | everything, plus invite, assign roles, rename and form partnerships |
| Engineer | build, operate and configure machines, research, launch rockets, change routes |
| Guest | open and view the agency's machines and containers, but take out, put in, configure or break nothing |
| Non-member | like a guest, at other agencies |

- **Dome protection:** in an active dome of another agency, non-members place and break no blocks, not even dirt or stone. Anyone may breathe there.
- Server **operators** are exempt from these rules, as with spawn protection.

## Roles and name

The leader manages the agency in the **Agency** tab:

- **Change role:** next to every member is a button with their role. One click switches between engineer and guest. The member gets a message.
- **Hand over leadership:** the "Leader" button makes the member leader. You yourself then become an engineer.
- **Rename:** enter a name at the bottom left and click "Rename". An empty name restores "<founder>'s Agency".

@tip Give players who should only watch the guest role. Then they cannot break anything by accident.

## Partners

Two agencies can become **partners**. Their cargo rockets may then supply each other: released [[aussenposten|cargo terminals]], released [[werft|shipyards]] and every [[handelsposten|trading post]].

1. The leader opens the **Partners** tab at the <item:infinite_horizon:research_terminal>. On the right are the agencies with someone online right now.
2. They click "Offer" next to an agency. Its members get a message.
3. The leader of the other agency sees the offer on the left under "Offers to you" and clicks "Accept".
4. The partnership only applies once both leaders have confirmed. Either leader can dissolve it again with "End".

Partners are not members: in other agencies' domes and at their machines they still only have guest rights.

@tip You decide yourself what a partner may supply: at the cargo terminal and the shipyard, the "Partners" button switches the release on or off. Trading posts are always open to partners.

## Commands

- `/infinite_horizon agency info` shows your agency: name, ID, members with role and visited dimensions. With a player name after it, it shows that player's agency.
- `/infinite_horizon agency transfer <from> <to>` (operators only): all property of the first agency passes to the second. Instead of an agency ID you may give a player name. The old agency loses its members. If one comes back, they get a new agency.

@tip If someone no longer plays on the server, an operator can give their machines to another agency with `transfer`. The blocks change owner by themselves at the next access; you do not have to place anything again.

@warning `transfer` cannot be undone: the old agency is empty afterwards, and its property belongs to the new one for good.
