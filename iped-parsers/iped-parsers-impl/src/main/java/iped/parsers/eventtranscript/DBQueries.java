package iped.parsers.eventtranscript;

public final class DBQueries {

    public static final String HISTORY = "SELECT"
        + " UserSID,"
        + " json_extract(JSONPayload,'$.data.CorrelationGuid') AS CorrelationGuid,"
        + " Timestamp,"
        + " replace(group_concat(DISTINCT TagName), ',', ';') AS TagNames,"
        + " replace(group_concat(DISTINCT EventName), ',', ';') AS EventNames,"
        + " URL,"
        + " ReferURL,"
        + " App,"
        + " replace(group_concat(DISTINCT nullif(PageTitle, '')), ',', ';') AS PageTitles,"
        + " JSONPayload"
        + " FROM ("
        + " SELECT events_persisted.sid AS UserSID,"
        + "     datetime((events_persisted.timestamp/10000000) - 11644473600, 'unixepoch', 'UTC') AS Timestamp,"
        + "     tag_descriptions.tag_name AS TagName,"
        + "     events_persisted.full_event_name AS FullEventName,"
        + "     replace(replace(substr(distinct events_persisted.full_event_name,39),'Microsoft.',''),'WebBrowser.HistoryJournal.','') AS 'EventName',"
        + "     json_extract(events_persisted.payload,'$.ext.app.name') AS App,"
        + "     events_persisted.compressed_payload_size AS CompressedPayloadSize,"
        + "     json_extract(events_persisted.payload,'$.data.navigationUrl') AS URL,"
        + "     json_extract(events_persisted.payload,'$.data.referUrl') AS ReferURL,"
        + "     json_extract(events_persisted.payload,'$.data.PageTitle') AS PageTitle,"
        + "     events_persisted.payload AS JSONPayload"
        + " FROM"
        + "     events_persisted"
        + "     LEFT JOIN event_tags ON events_persisted.full_event_name_hash = event_tags.full_event_name_hash"
        + "     LEFT JOIN tag_descriptions ON event_tags.tag_id = tag_descriptions.tag_id"
        + "     INNER JOIN provider_groups ON events_persisted.provider_group_id = provider_groups.group_id"
        + " WHERE"
        + "     (tag_descriptions.tag_name='Browsing History' AND events_persisted.full_event_name LIKE '%HistoryAddUrl') OR"
        + "     (tag_descriptions.tag_name='Product and Service Usage' AND events_persisted.full_event_name LIKE '%HistoryAddUrlEx')"
        + " )"
        + " GROUP BY CorrelationGuid"
        + " ORDER BY Timestamp DESC";

        public static final String INVENTORY_APPS = "SELECT"
        + " datetime( ( events_persisted.timestamp / 10000000 ) - 11644473600, 'unixepoch' ) AS 'Timestamp',"
        + " json_extract(events_persisted.payload,'$.ext.utc.seq') as 'seq',"
        + " tag_descriptions.tag_name AS TagName,"
        + " replace(events_persisted.full_event_name,'Microsoft.Windows.Inventory.Core.Inventory','') as 'EventName',"
        + " events_persisted.full_event_name as 'FullEventName',"
        + " json_extract(events_persisted.payload,'$.data.Type') as 'Type',"
        + " json_extract(events_persisted.payload,'$.data.Name') as 'Name',"
        + " json_extract(events_persisted.payload,'$.data.PackageFullName') as 'PackageFullName',"
        + " json_extract(events_persisted.payload,'$.data.Version') as 'Version',"
        + " json_extract(events_persisted.payload,'$.data.Publisher') as 'Publisher',"
        + " json_extract(events_persisted.payload,'$.data.RootDirPath') as 'RootDirPath',"
        + " json_extract(events_persisted.payload,'$.data.HiddenArp') as 'HiddenArp',"
        + " json_extract(events_persisted.payload,'$.data.InstallDate') as 'InstallDate',"
        + " json_extract(events_persisted.payload,'$.data.Source') as 'Source',"
        + " json_extract(events_persisted.payload,'$.data.OSVersionAtInstallTime') as 'OSVersionAtInstallTime',"
        + " json_extract(events_persisted.payload,'$.data.InstallDateMsi') as 'MsiInstallDate',"
        + " json_extract(events_persisted.payload,'$.data.MsiPackageCode') as 'MsiPackageCode',"
        + " json_extract(events_persisted.payload,'$.data.MsiProductCode') as 'MsiProductCode',"
        + " case json_extract(events_persisted.payload,'$.data.baseData.action') "
        + "     when 1 then 'Add'"
        + "     when 2 then 'Remove'"
        + "     else json_extract(events_persisted.payload,'$.data.baseData.action') "
        + " end as 'action',"
        + " json_extract(events_persisted.payload,'$.data.baseData.objectInstanceId') as 'InstanceId',"
        + " trim(json_extract(events_persisted.payload,'$.ext.user.localId'),'m:') as 'UserId',"
        + " sid as 'UserSID',"
        + " events_persisted.payload AS JSONPayload"
        + " FROM"
        + "     events_persisted"
        + "     LEFT JOIN event_tags ON events_persisted.full_event_name_hash = event_tags.full_event_name_hash"
        + "     LEFT JOIN tag_descriptions ON event_tags.tag_id = tag_descriptions.tag_id"
        + " WHERE"
        + " events_persisted.full_event_name like 'Microsoft.Windows.Inventory.Core.Inventory%'"
        + " order by cast('InstallDate' as integer) desc";

    public static final String APP_INTERACTIVITY = "SELECT"
    + " datetime( ( events_persisted.timestamp / 10000000 ) - 11644473600, 'unixepoch' ) AS 'Timestamp',"
    + " json_extract(events_persisted.payload,'$.ext.utc.seq') as 'seq',"
    + " tag_descriptions.tag_name as 'TagName',"
    + " replace(events_persisted.full_event_name,'Win32kTraceLogging.','') as 'EventName',"
    + " replace(substr(json_extract(events_persisted.payload,'$.data.AggregationStartTime'),1,19), 'T', ' ') as 'AggregationStartTime', "
    + " json_extract(events_persisted.payload,'$.data.AggregationDurationMS') as 'AggregationDurationMS', "
    + " case when substr(json_extract(events_persisted.payload,'$.data.AppId'),1,1) is 'W' "
    + "      then substr(json_extract(events_persisted.payload,'$.data.AppId'),93)"
    + "      when substr(json_extract(events_persisted.payload,'$.data.AppId'),1,1) is 'U' "
    + "      then substr(json_extract(events_persisted.payload,'$.data.AppId'),3)"
    + "      else json_extract(events_persisted.payload,'$.data.AppId') "
    + "      end as 'AppId',"
    + " case when substr(json_extract(events_persisted.payload,'$.data.AppId'),1,1) is 'W' "
    + "     then substr(json_extract(events_persisted.payload,'$.data.AppVersion'),21,(instr(substr(json_extract(events_persisted.payload,'$.data.AppVersion'),21),'!')-1) )"
    + "     end as 'PE Header CheckSum',	"
    + " case substr(json_extract(events_persisted.payload,'$.data.AppId'),1,1) "
    + "     when 'W' then 'Win' "
    + "     when 'U' then 'UWP' "
    + "     end as 'Type',	"
    + " json_extract(events_persisted.payload,'$.data.WindowWidth')||'x'||json_extract(events_persisted.payload,'$.data.WindowHeight') as 'WindowSize(WxH)',	"
    + " json_extract(events_persisted.payload,'$.data.MouseInputSec') as 'MouseInputSec', "
    + " json_extract(events_persisted.payload,'$.data.InFocusDurationMS') as 'InFocusDurationMS', "
    + " json_extract(events_persisted.payload,'$.data.UserActiveDurationMS') as 'UserActiveDurationMS', "
    + " json_extract(events_persisted.payload,'$.data.SinceFirstInteractivityMS') as 'SinceFirstInteractivityMS', "
    + " json_extract(events_persisted.payload,'$.data.UserOrDisplayActiveDurationMS') as 'UserOrDisplayActiveDurationMS', "
    + " json_extract(events_persisted.payload,'$.data.FocusLostCount') as 'FocusLostCount',"
    + " case when substr(json_extract(events_persisted.payload,'$.data.AppId'),1,1) is 'W'	"
    + "     then upper(substr(json_extract(events_persisted.payload,'$.data.AppId'),52,40)) "
    + "     end as 'SHA1',	"
    + " case when substr(json_extract(events_persisted.payload,'$.data.AppId'),1,1) is 'W'	"
    + "    then upper(substr(json_extract(events_persisted.payload,'$.data.AppId'),3,44)) "
    + "    end as 'ProgramId',"
    + " upper(json_extract(events_persisted.payload,'$.data.AppSessionId')) as 'AppSessionId',"
    + " trim(json_extract(events_persisted.payload,'$.ext.user.localId'),'m:') as 'UserId',"
    + " sid as 'UserSID',"
    + " logging_binary_name,"
    + " events_persisted.payload AS JSONPayload"
    + " from events_persisted "
    + " join event_tags on events_persisted.full_event_name_hash = event_tags.full_event_name_hash"
    + " join tag_descriptions on event_tags.tag_id = tag_descriptions.tag_id "
    + " where "
    + "  events_persisted.full_event_name in ('Win32kTraceLogging.AppInteractivity','Win32kTraceLogging.AppInteractivitySummary' )"
    + " order by cast(events_persisted.timestamp as integer) desc";

}
