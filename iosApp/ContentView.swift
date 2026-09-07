import Foundation
import SwiftUI
import CargaCercaShared

@MainActor
final class CatalogViewModel: ObservableObject {
    let catalog = CargaCercaCatalog()

    @Published var isLoading = false
    @Published var loadMessage = "Datos compartidos listos"
    @Published var revision = 0

    private var hasLoaded = false

    func loadMadridIfNeeded() {
        guard !hasLoaded else { return }
        hasLoaded = true
        loadNearby(latitude: 40.4168, longitude: -3.7038)
    }

    func reloadMadrid() {
        loadNearby(latitude: 40.4168, longitude: -3.7038)
    }

    private func loadNearby(latitude: Double, longitude: Double) {
        isLoading = true
        loadMessage = "Buscando cargadores reales…"

        catalog.loadNearby(latitude: latitude, longitude: longitude) { success in
            DispatchQueue.main.async { [weak self] in
                guard let self else { return }
                self.isLoading = false
                self.loadMessage = success
                    ? "OpenStreetMap · datos reales cargados"
                    : "Sin conexión · usando datos demo"
                self.revision += 1
            }
        }
    }
}

struct ContentView: View {
    @StateObject private var viewModel = CatalogViewModel()

    private var catalog: CargaCercaCatalog { viewModel.catalog }

    var body: some View {
        ZStack {
            Color(red: 0.025, green: 0.055, blue: 0.09)
                .ignoresSafeArea()

            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    header
                    sharedBadge
                    dataStatus
                    stationList
                }
                .padding(20)
            }
            .refreshable {
                viewModel.reloadMadrid()
            }
        }
        .preferredColorScheme(.dark)
        .onAppear {
            viewModel.loadMadridIfNeeded()
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("CargaCerca")
                .font(.system(size: 32, weight: .black, design: .rounded))
                .foregroundStyle(Color.cyan)
            Text("iPhone · Android + iOS")
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
                Text("shared v\(catalog.sharedVersion()) · misma lógica que Android")
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

    private var dataStatus: some View {
        HStack(spacing: 12) {
            if viewModel.isLoading {
                ProgressView()
                    .tint(.cyan)
            } else {
                Image(systemName: catalog.isUsingRemoteData() ? "network" : "externaldrive")
                    .foregroundStyle(catalog.isUsingRemoteData() ? Color.green : Color.orange)
            }

            VStack(alignment: .leading, spacing: 2) {
                Text(viewModel.loadMessage)
                    .font(.subheadline.weight(.semibold))
                Text("Madrid como ubicación inicial · desliza hacia abajo para actualizar")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
            Spacer()
        }
        .padding(14)
        .background(Color.white.opacity(0.045), in: RoundedRectangle(cornerRadius: 16))
    }

    private var stationList: some View {
        let _ = viewModel.revision
        return VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Cargadores cercanos")
                    .font(.title3.bold())
                Spacer()
                Text("\(Int(catalog.stationCount()))")
                    .font(.caption.bold())
                    .foregroundStyle(Color.cyan)
                    .padding(.horizontal, 9)
                    .padding(.vertical, 5)
                    .background(Color.cyan.opacity(0.12), in: Capsule())
            }

            ForEach(0..<Int(catalog.stationCount()), id: \.self) { index in
                stationCard(index: index)
            }
        }
    }

    private func stationCard(index: Int) -> some View {
        let kotlinIndex = Int32(index)
        let power = Int(catalog.stationPowerKw(index: kotlinIndex))
        let distance = catalog.stationDistanceKm(index: kotlinIndex)
        let remote = catalog.stationDataSource(index: kotlinIndex) == "OpenStreetMap"

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
                Text(remote ? "REAL" : "DEMO")
                    .font(.caption2.bold())
                    .foregroundStyle(remote ? Color.green : Color.orange)
                    .padding(.horizontal, 9)
                    .padding(.vertical, 5)
                    .background((remote ? Color.green : Color.orange).opacity(0.12), in: Capsule())
            }

            Text(catalog.stationAddress(index: kotlinIndex))
                .font(.caption)
                .foregroundStyle(.secondary)

            HStack(spacing: 8) {
                metric("bolt.fill", power > 0 ? "\(power) kW" : "Potencia —")
                metric("location.fill", String(format: "%.1f km", distance))
                metric("bolt.car.fill", catalog.stationConnector(index: kotlinIndex))
            }

            Text(catalog.stationAvailabilityLabel(index: kotlinIndex))
                .font(.caption2)
                .foregroundStyle(.secondary)
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
            .padding(.horizontal, 8)
            .padding(.vertical, 7)
            .background(Color.white.opacity(0.05), in: Capsule())
    }
}
