import SwiftUI
import CargaCercaShared

struct ContentView: View {
    private let catalog = CargaCercaCatalog()

    var body: some View {
        ZStack {
            Color(red: 0.025, green: 0.055, blue: 0.09)
                .ignoresSafeArea()

            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    header
                    sharedBadge
                    stationList
                }
                .padding(20)
            }
        }
        .preferredColorScheme(.dark)
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("CargaCerca")
                .font(.system(size: 32, weight: .black, design: .rounded))
                .foregroundStyle(Color.cyan)
            Text("iPhone · primera base multiplataforma")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
    }

    private var sharedBadge: some View {
        HStack(spacing: 10) {
            Image(systemName: "arrow.triangle.2.circlepath")
                .foregroundStyle(Color.cyan)
            VStack(alignment: .leading, spacing: 2) {
                Text("Kotlin Multiplatform conectado")
                    .font(.headline)
                Text("shared v\(catalog.sharedVersion()) · mismos modelos que Android")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            Image(systemName: "checkmark.circle.fill")
                .foregroundStyle(.green)
        }
        .padding(16)
        .background(Color.white.opacity(0.06), in: RoundedRectangle(cornerRadius: 20))
        .overlay(
            RoundedRectangle(cornerRadius: 20)
                .stroke(Color.cyan.opacity(0.25), lineWidth: 1)
        )
    }

    private var stationList: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Cargadores compartidos")
                .font(.title3.bold())

            ForEach(0..<Int(catalog.stationCount()), id: \.self) { index in
                stationCard(index: index)
            }
        }
    }

    private func stationCard(index: Int) -> some View {
        let kotlinIndex = Int32(index)
        let power = Int(catalog.stationPowerKw(index: kotlinIndex))
        let distance = catalog.stationDistanceKm(index: kotlinIndex)

        return VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(catalog.stationName(index: kotlinIndex))
                        .font(.headline)
                    Text(catalog.stationOperator(index: kotlinIndex))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                Text(catalog.stationAvailabilityLabel(index: kotlinIndex))
                    .font(.caption2.bold())
                    .foregroundStyle(.green)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(Color.green.opacity(0.12), in: Capsule())
            }

            Text(catalog.stationAddress(index: kotlinIndex))
                .font(.caption)
                .foregroundStyle(.secondary)

            HStack(spacing: 10) {
                metric("bolt.fill", power > 0 ? "\(power) kW" : "Potencia —")
                metric("location.fill", String(format: "%.1f km", distance))
                metric("ev.charger.fill", catalog.stationConnector(index: kotlinIndex))
            }
        }
        .padding(16)
        .background(Color.white.opacity(0.055), in: RoundedRectangle(cornerRadius: 20))
        .overlay(
            RoundedRectangle(cornerRadius: 20)
                .stroke(Color.white.opacity(0.08), lineWidth: 1)
        )
    }

    private func metric(_ icon: String, _ text: String) -> some View {
        Label(text, systemImage: icon)
            .font(.caption2.weight(.semibold))
            .foregroundStyle(Color.white.opacity(0.88))
            .padding(.horizontal, 9)
            .padding(.vertical, 7)
            .background(Color.white.opacity(0.05), in: Capsule())
    }
}
