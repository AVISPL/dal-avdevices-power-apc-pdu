# APC PDUs Integration - Capabilities & Configuration
This document covers APC PDUs Adapter Capabilities and Configuration.

Symphony integrates with APC Power Distribution Units (PDUs) to provide monitoring and control of networked APC PDUs via SSH. The adapter exposes power outlet control, energy metering, overload thresholds, and per-outlet configuration.

Main features are: per-outlet power switching and reboot, PDU-level energy metering, overload and load warning thresholds, and cold start delay configuration.

## APC PDUs - Main use cases
- **Monitor** PDU power metrics (active power, apparent power, current), firmware versions, input type, and outlet count
- **Control** individual outlet power states, reboot outlets, and configure power-on/off delays and reboot duration
- **Track** PDU-level load thresholds — low load warnings, near-overload warnings, and overload alarms
- **Inventory** APC PDU models and outlet configurations across installations

## APC PDUs - Supported Models
AP7900, AP7920, AP8659 UPS, AP5401, AP5405, AP5456

**Note:** The set of available monitoring and control properties varies by device model and input type (single-phase, banked, or 3-phase).

## APC PDUs - Device Configuration

The APC PDUs Adapter communicates with the device over SSH using device credentials.

| Field | Description |
|---|---|
| Device Type | AV Devices |
| Category | Power |
| Manufacturer | APC |
| Model | e.g., AP7900 |
| Monitoring Service | Advanced Monitoring |
| Ping Protocol | TCP |
| Monitoring Source | Direct |
| Management Address | IP address of the APC PDU |
| Protocol | SSH |
| Username | SSH username |
| Password | SSH password |
| Port Number | 22 (default) |

### APC PDUs - Adapter configuration properties

| Property | Description |
|---|---|
| historicalProperties | Comma-separated list of properties to track historically. For non-3-phase: `Current(A)`. For 3-phase: `Phase1Current(A)`, `Phase2Current(A)`, `Phase3Current(A)`. Default: blank. |

## APC PDUs - Available Monitored Data

### General properties

| Property | Description |
|---|---|
| ActivePower(W) | Actual power consumption in Watts |
| ApparentPower(VA) | Apparent power in Volt-Amperes |
| InputType | Input power configuration: single-phase, banked, or 3-phase |
| Current(A) | Total input current — non-3-phase only |
| Phase1/2/3Current(A) | Per-phase current — 3-phase only |
| MaximumLoadCurrent(A) | Maximum allowable current |
| OutletTotal | Total number of outlets on the PDU |
| Model, AOSVersion, PDUVersion | Device identity and firmware versions |

### Adapter Metadata
AdapterBuildDate, AdapterVersion, AdapterUptime, AdapterUptime(min)

### Configuration properties (PDU-level)

| Property | Description |
|---|---|
| ColdStartDelay | Toggle (On/Off) — enables cold start delay. When Off, ColdStartDelay(sec) is set to 0. |
| ColdStartDelay(sec) | Delay before outlet powers on after cold start (0–300s). Configurable only when ColdStartDelay is On. |
| LowLoadWarning(A) | Current threshold below which a low-load warning triggers |
| NearOverloadWarning(A) | Current threshold for near-overload warning |
| OverloadAlarm(A) | Current threshold for overload alarm |
| OverloadRestriction | Toggle (On/Off) — enables automatic restriction on overload |

For 3-phase PDUs, these properties are prefixed per phase — for example: Phase1LowLoadWarning(A), Phase2OverloadAlarm(A), Phase3OverloadRestriction.

### Per-outlet properties
Outlets are named using the pattern Outlet_Number, zero-padded to two digits (e.g., Outlet_01), numbered sequentially from 1 up to the device's reported outlet count — independent of phase.

| Property | Description |
|---|---|
| Name | User-defined outlet label |
| PowerStatus | Current outlet state: On or Off — also controllable |
| Reboot | Removes then restores power to the outlet |
| RebootDuration(sec) | How long the outlet stays off during reboot |
| PowerOffDelay | Toggle (On/Off) — enables power-off delay. When Off, PowerOffDelay(sec) is always 0. |
| PowerOffDelay(sec) | Delay before outlet powers off. Configurable only when PowerOffDelay is On. |
| PowerOnDelay | Toggle (On/Off) — enables power-on delay. When Off, PowerOnDelay(sec) is set to 0. |
| PowerOnDelay(sec) | Delay before outlet powers on. Configurable only when PowerOnDelay is On. |

## APC PDUs - Troubleshooting

**Login / Connection Error**
- Verify the SSH username and password are correct for the APC PDU
- Confirm the Management Address is the correct IP of the device
- Ensure port 22 (or the configured SSH port) is reachable from the Cloud Connector

**No Data / Missing Properties**
- Confirm the PDU model is in the supported list (AP7900, AP7920, AP8659, AP5401, AP5405, AP5456)
- Phase current properties (Phase1/2/3Current) only appear when InputType is 3-phase; Current(A) only appears for non-3-phase

**Link Error / Ping Timeout**
- Verify network reachability between the Cloud Connector and the PDU
- Confirm Ping Protocol is set to TCP in the Symphony device configuration

**Per-Outlet Delay Properties Not Editable**
- PowerOffDelay(sec) and PowerOnDelay(sec) are only configurable when their respective toggle properties (PowerOffDelay, PowerOnDelay) are set to On

If none of the recommended steps help, please enter an SOS ticket at {https://avi-spl.atlassian.net/servicedesk/customer/portals}

## APC PDUs - What AI Assistant can do with it:
- Find APC PDU devices (AV Devices | Power | APC) in Symphony
- Verify APC PDUs adapter configuration
- Report on outlet power states, PDU energy metrics, and load warning thresholds

## APC PDUs - What AI Assistant cannot do with it:
- Provision devices
- Configure APC PDU network or SSH settings directly on the device
- Push firmware updates
