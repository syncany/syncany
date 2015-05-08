
// 1. Set JAVA_HOME env. variable ///////////

const 
  ModPathRun = True; 
  ModPathType = 'user'; 
  
var
  JavaHome: String;
  JavaHomeSet: Boolean;
  RegRootSearchPaths: TArrayOfString;
  RegRootSearchPathsLimitMemory: TArrayOfBoolean;
  LimitMemory: Boolean;

function GetJavaHomeForRegPath(RegRootKey: Integer; RegRootPath: String): String;
var 
  TempJavaHome, TempJavaVersion: String;
begin
  Result := '';
    
  if RegValueExists(RegRootKey, RegRootPath, 'CurrentVersion') then begin
    RegQueryStringValue(RegRootKey, RegRootPath, 'CurrentVersion', TempJavaVersion);
      
    if RegValueExists(RegRootKey, RegRootPath + '\' + TempJavaVersion, 'JavaHome') then begin
      RegQueryStringValue(RegRootKey, RegRootPath + '\' + TempJavaVersion, 'JavaHome', TempJavaHome);
          
      Result := TempJavaHome;
    end        
  end
end;

function InitializeSetup(): Boolean;
var 
  RegRootKey, i: Integer;
  RegRootPath, RegRootJavaHome: String;  
begin
  // Search paths for the Java installation
  SetArrayLength(RegRootSearchPaths, 4); 
  RegRootSearchPaths[0] := 'SOFTWARE\JavaSoft\Java Development Kit';
  RegRootSearchPaths[1] := 'SOFTWARE\JavaSoft\Java Runtime Environment'; 
  RegRootSearchPaths[2] := 'SOFTWARE\Wow6432Node\JavaSoft\Java Development Kit'; 
  RegRootSearchPaths[3] := 'SOFTWARE\Wow6432Node\JavaSoft\Java Runtime Environment'; 

  // Limit memory due to 32-bit Java on 64-bit system (see issue #222)
  // The values correspond to the paths above.
  SetArrayLength(RegRootSearchPathsLimitMemory, 4);
  RegRootSearchPathsLimitMemory[0] := False;
  RegRootSearchPathsLimitMemory[1] := False;
  RegRootSearchPathsLimitMemory[2] := True;
  RegRootSearchPathsLimitMemory[3] := True;

  // Registry root depending on 32/64 bit system
  if IsWin64 then begin
    RegRootKey := HKLM64;
  end
  else begin
    RegRootKey := HKLM;
  end;
  
  // Find JAVA_HOME and memory limit (32-bit Java on 64-bit Windows)
  JavaHome := GetEnv('JAVA_HOME');
  
  if JavaHome = '' then begin
    // JAVA_HOME already set, now only find the memory limit

    JavaHomeSet := True;
    Result := True;

    for i := 0 to GetArrayLength(RegRootSearchPaths)-1 do begin
      RegRootJavaHome := GetJavaHomeForRegPath(RegRootKey, RegRootSearchPaths[i]);

      if RegRootJavaHome = JavaHome then begin
        LimitMemory := RegRootSearchPathsLimitMemory[i];
      end
    end

    Log('JAVA_HOME is already set: ' + JavaHome);
  end
  else begin
    // JAVA_HOME not set, find by trying all registry keys above
    JavaHomeSet := False;
    Result := False;

    for i := 0 to GetArrayLength(RegRootSearchPaths)-1 do begin
      RegRootPath := GetJavaHomeForRegPath(RegRootKey, RegRootSearchPaths[i]);
      LimitMemory := RegRootSearchPathsLimitMemory[i];
       
      if RegRootPath <> '' then begin
        Result := True;
        break;
      end
    end

    if Result = False then begin
      MsgBox('Java is not installed on your computer.'#13'Please install Java from java.com try again.', MbError, Mb_Ok);
    end

    Log('JAVA_HOME not set; will be set to: ' + JavaHome);
  end
end;

procedure SetJavaHome();
begin
  if not JavaHomeSet then begin
    Log('Setting JAVA_HOME: ' + JavaHome);
    RegWriteStringValue(HKEY_CURRENT_USER, 'Environment', 'JAVA_HOME', JavaHome);
  end
end;

// 2. Set PATH variable (if ticked)  ////////

function ModPathDir(): TArrayOfString; 
begin
  setArrayLength(Result, 1); 
  Result[0] := ExpandConstant('{app}\bin'); 
  
  Log('PATH will be extended by ' + ExpandConstant('{app}\bin'));
end; 
        
#include "modpath.iss"

// 3. Write memory limit file (on post install) ////////

procedure WriteMemoryLimitFile();
var 
  UserConfigFileName, UserConfigXml: String;
  TagMaxMemoryPosStart, TagMaxMemoryPosEnd, TagUserConfigEnd: Integer;
begin
  if LimitMemory then begin
    UserConfigFileName := ExpandConstant('{userappdata}\Syncany\userconfig.xml');

    // If userconfig.xml exists, replace <maxMemory>-tags with maximum limit
    if FileExists(UserConfigFileName) then begin
      if LoadStringFromFile(UserConfigFileName, UserConfigXml) then begin
        // Remove <maxMemory>..</maxMemory> 
        TagMaxMemoryPosStart := Pos('<maxMemory>', UserConfigXml);
        TagMaxMemoryPosEnd := Pos('</maxMemory>', UserConfigXml) + Length('</maxMemory>');

        if (TagMaxMemoryPosStart > 0) and (TagMaxMemoryPosEnd > 0) then begin
          UserConfigXml := Copy(UserConfigXml, 1, TagMaxMemoryPosStart-1) + Copy(UserConfigXml, TagMaxMemoryPosEnd, Length(UserConfigXml));
        end
       
        // Add <maxMemory>1300M</maxmemory>
        TagUserConfigEnd := Pos('</userConfig>', UserConfigXml);

        if TagUserConfigEnd > 0 then begin
          UserConfigXml := Copy(UserConfigXml, 1, TagUserConfigEnd-1) + '   <maxMemory>512M</maxMemory>'#13#10'</userConfig>';
          SaveStringToFile(UserConfigFileName, UserConfigXml, False);
        end
      end
    end

    // If userconfig.xml does not exist, create new file
    else begin
      SaveStringToFile(UserConfigFileName, '<userConfig>'#13#10'   <maxMemory>512M</maxMemory>'#13#10'</userConfig>', False);           
    end
  end
end;

// 4. Trigger post-install (taken from modpath.iss) /////////

procedure CurStepChanged(CurStep: TSetupStep);
begin
  if CurStep = ssPostInstall then begin
    if ModPathRun then begin
      ModPath(); // In 'modpath.iss', calls 'ModPathDir()'
    end

    SetJavaHome();
    WriteMemoryLimitFile();
  end
end;

procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
begin
	if CurUninstallStep = usUninstall then begin
		if ModPathRun then begin
		  ModPath(); // In 'modpath.iss', calls 'ModPathDir()'
	  end;		
	end;
end;
