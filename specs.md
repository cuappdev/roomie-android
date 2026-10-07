# Roomie 
## Goal
Fostering easier communication between roommates regarding spending, 
scheduling, communication, and chore/task/shopping management.

## Features
- Sign in with a Cornell NetID only (`<netid>@cornell.edu`). Non-Cornell accounts cannot join a house.
- Create a house and invite roommates by Cornell NetID (they get an email at their @cornell.edu address), or join with an invite code.

### Possible Ideas
- Bill tracking. Possibly, once some sort of grocery shopping
is done, some sort of bill splitting could be done where how much each person owes could be tracked
- Total money owed. That bill tracking above could stack and be organied per person.

## Design (Figma: Roomie-FA26 → "MVP Design", node 1:2)
https://www.figma.com/design/MixBrFpwBtU7ZR649q564D/Roomie-FA26?node-id=1-2

Frames are 390×844 (phone). Warm cream background, brown primary buttons, pastel sticky notes.
Bottom tab bar: 4 icon-only tabs — Home, Chores, Note board, Shopping (each needs an accessible name).
Settings gear on each tab header opens **Me**.

### Onboarding (5 steps)
1. **Welcome / Sign in** — Cornell NetID (`@cornell.edu` suffix) + password. Only Cornell accounts can join a house.
   "Got an invite code? Join your roommates' house directly" shortcut.
2. **Pick your house buddy** — avatar picker: Mug, Sock, Plant, Toaster, Teapot. Shown next to your name; changeable later in Me. Next / Skip for now.
3. **How are you setting up?** — "Start a house" (name it, add roommates by NetID) or "Join with an invite code" (e.g. `LOFT-0000`).
4. **Add roommates** — add by NetID; each row shows status (Joined / Invite sent) and can be removed. Invitees get an email and pick their own buddy.
5. **Set up your house** — house name, invite code (Copy, Share invite link), roommate list with Owner / Joined / Pending. "Enter your house".

### Home
- Roommate avatar row.
- **Coming up** — the next 2 notes with a due date, soonest first (e.g. "17 Nov · Plumber visit").
- Note board preview — at most 6 stickies: pinned first, then upcoming due date, then newest. Extras collapse into a "+N more" stack that opens the full Note board. "Add note".
- **This Week's Tasks** — day, chore, assignee.

### Note board
- Grid of sticky notes ("Pinned first, then due soon, then newest"), scrolls, never hides notes.
- "Older notes (N)" section. "Add note".
- **New note** bottom sheet: text (max 140 chars, counter), optional due date, "Pin to top for 7 days" toggle (max 3 pins, shows "2 of 3 pins in use"), Post note.
- Rules: stickies truncate at 3 lines (tap to read all); unpinned notes with no upcoming date move to Older notes after 14 days; dated notes appear in Coming up until the day passes.

### Chores
- **Today** (checkable) and **Upcoming** (Tomorrow / Friday / Saturday …) with assignee.
- **Weekly Chores** — one row per weekday (S M T W Th F S), chore + assignee dropdown, "Nothing planned" when empty. "Add chore".
- **New chore** screen: chore name, assign to (avatar picker), due date, optional notes, Add chore.

### Shopping list
- Banner when someone is shopping ("Mina is at the store").
- Add an item field; "Add again" chips from recent purchases.
- **Needed · N** — checkbox items with who added / note / quantity (×2) / "Couldn't find" flag.
- **Bought · N** — "Clears in 7 days"; undo snackbar after marking bought.
- Duplicate check while typing: shows matches on current list and last 30 days ("Already on the list · Make it ×2", "Bought 2 days ago · Add again", "Add 'milk' as a new item").
- **Edit item** sheet: name, quantity stepper, optional note, "Added by …", Couldn't find it, Delete, Save.
- Empty state: "Nothing on the list" + Add first item.

### Me (profile)
- Avatar, name, NetID, Change pic.
- House card: name, roommate count, Edit, roommate list with Owner badge.
- Invite code + Copy, Share invite link, Sign out, Leave house.

### Assets
Avatars: avatar-mug, avatar-plant, avatar-sock, avatar-teapot, avatar-toaster (512×512, pastel circle backgrounds).

### Out of scope for MVP (room left in layout)
Shared calendar, emoji reactions/moods, shared-item inventory, recurring chores & rotation, push notifications, bill tracker, dark mode (colors are tokenized so dark mode is a later variable swap).
