require "formula"

class Syncany < Formula
  homepage "https://www.syncany.org"
  url "https://codeload.github.com/syncany/syncany/tar.gz/v0.4.3-alpha"
  sha256 "1e749bf505c0e2dcecfbb08b929711dc179b70b44d5f93e8e2558f628abcb735"
  version "0.4.3-alpha"
  head "https://github.com/syncany/syncany.git", :branch => "develop"

  depends_on :java => "1.7"
  depends_on "gradle" => "2.2"

  def install
    system "./gradlew", "installApp"

    inreplace "build/install/syncany/bin/syncany" do |s|
      s.gsub! /APP_HOME="`pwd -P`"/, %{APP_HOME="#{libexec}"}
    end

    cd "build/install/syncany/bin" do
      rm Dir["*.bat"] # Windows batch scripts
      rm "syncany" # This is identical to the sy script, and the docs mostly refer to the sy script.
    end

    libexec.install Dir["build/install/syncany/*"]
    bin.install_symlink Dir["#{libexec}/bin/sy"]
  end

  plist_options :manual => "sy --daemon"

  def plist; <<-EOS.undent
    <?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
    <plist version="1.0">
    <dict>
      <key>Label</key>
      <string>#{plist_name}</string>
      <key>ProgramArguments</key>
      <array>
        <string>#{prefix}/bin/sy</string>
        <string>--daemon</string>
      </array>
      <key>ProcessType</key>
      <string>Background</string>
      <key>KeepAlive</key>
      <true/>
    </dict>
    </plist>
    EOS
  end

  test do
    require 'open3'
    require 'expect'

    source, target, repo = 'source', 'target', 'repo'

    [source, target, repo].each { |p| mkdir(p) }

    options = "-P local -o path=../#{repo}"

    commands = {
      :init => "#{bin}/sy init #{options}",
      :up => "#{bin}/sy up",
      :connect => "#{bin}/sy connect #{options}",
      :down => "#{bin}/sy down"
    }

    password = "password-123"
    test_data, test_file = 'Howdy, howdy, howdy.', 'README.txt'
    timeout = 5

    Dir.chdir(source) do
      puts "-- Initializing #{source}"
      Open3.popen3(commands[:init]) do |i, o, _, _|
        o.sync = true
        i.sync = true

        o.expect("Password (min. 10 chars):", timeout) do |result|
          i.puts "#{password}\n"
        end

        o.expect("Confirm:", timeout) do |result|
          i.puts "#{password}\n"
        end
      end

      puts "-- Creating test file"
      File.open(test_file, "w") { |f| f << test_data }

      puts "-- Syncing #{source}"
      Open3.popen3(commands[:up]) { }

    end

    Dir.chdir(target) do
      puts "-- Connecting #{target}"
      Open3.popen3(commands[:connect]) do |i, o, _, _|
        o.sync = true
        i.sync = true

        o.expect("Password:", timeout) do |result|
          i.puts "#{password}\n"
        end
      end

      puts "-- Syncing #{target}"
      Open3.popen3(commands[:down]) { }

      puts "-- Verifying test file"
      raise "Syncany test failed" unless File.read(test_file) == test_data
    end
  end
end
