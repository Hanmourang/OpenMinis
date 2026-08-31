import XCTest
@testable import Minis

final class WorkEnvironmentTests: XCTestCase {
    func testParseHostPortWithExplicitPort() {
        let parsed = DesktopConnectionManager.parseHostPort("192.168.1.8:18765")
        XCTAssertEqual(parsed?.host, "192.168.1.8")
        XCTAssertEqual(parsed?.port, 18765)
    }

    func testParseHostPortDefaultPort() {
        let parsed = DesktopConnectionManager.parseHostPort("office.local")
        XCTAssertEqual(parsed?.host, "office.local")
        XCTAssertEqual(parsed?.port, DesktopConnectionManager.defaultPort)
    }

    func testParseHostPortRejectsEmpty() {
        XCTAssertNil(DesktopConnectionManager.parseHostPort("   "))
        XCTAssertNil(DesktopConnectionManager.parseHostPort("nohost"))
    }

    func testParseHostPortRejectsBadPort() {
        XCTAssertNil(DesktopConnectionManager.parseHostPort("host:99999"))
        XCTAssertNil(DesktopConnectionManager.parseHostPort("host:abc"))
    }

    func testKindRoundTrip() {
        XCTAssertEqual(WorkEnvironmentKind(rawValue: "cloud"), .cloud)
        XCTAssertEqual(WorkEnvironmentKind(rawValue: "desktop"), .desktop)
        XCTAssertNil(WorkEnvironmentKind(rawValue: "other"))
    }
}
